package com.example.orderservice.api.service;

import com.example.orderservice.api.client.BookDto;
import com.example.orderservice.api.client.ProductClient;
import com.example.orderservice.api.exception.InvalidOrderStateException;
import com.example.orderservice.api.model.OrderEntity;
import com.example.orderservice.api.model.OrderItemEntity;
import com.example.orderservice.api.model.OrderStatus;
import com.example.orderservice.api.repository.OrderRepository;
import com.example.orderservice.api.request.OrderItemRequest;
import com.example.orderservice.api.request.OrderRequest;
import com.example.orderservice.api.response.OrderItemResponse;
import com.example.orderservice.api.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional(readOnly = true)   // Okuma metotları: oturum açık kalır, LAZY kalemlere güvenle erişilir
public class OrderService {
    // Her farklı status/page/size/sort yeni bir anahtar açar; bellek şişmesin diye sınır koyuyoruz
    private static final int MAX_PAGE_CACHE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    // Cache 1: sipariş id -> sipariş (DTO tutuyoruz, entity değil: LAZY sorunu çıkmasın)
    private final Map<Long, OrderResponse> orderCache = new ConcurrentHashMap<>();
    // Cache 2: sayfa isteği (status filtresi + page/size/sort) -> o sayfanın sonucu
    private final Map<PageKey, Page<OrderResponse>> pageCache = new ConcurrentHashMap<>();

    private record PageKey(OrderStatus status, Pageable pageable) {
    }

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    // status verilirse sadece o durumdaki siparişler gelir
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        PageKey key = new PageKey(status, pageable);
        Page<OrderResponse> cached = pageCache.get(key);
        if (cached != null) {
            log.info("CACHE HIT: sipariş sayfası {}", key);
            return cached;
        }
        log.info("CACHE MISS: sipariş sayfası {}", key);
        Page<OrderEntity> page;
        if (status == null) {
            page = orderRepository.findAll(pageable);
        } else {
            page = orderRepository.findByStatus(status, pageable);
        }
        Page<OrderResponse> response = page.map(this::mapToResponse);
        if (pageCache.size() >= MAX_PAGE_CACHE_SIZE) {
            pageCache.clear();          // basit strateji: dolunca hepsini boşalt
        }
        pageCache.put(key, response);
        return response;
    }

    public OrderResponse getOrderById(Long id) {
        OrderResponse cached = orderCache.get(id);
        if (cached != null) {
            log.info("CACHE HIT: sipariş id={}", id);
            return cached;
        }
        log.info("CACHE MISS: sipariş id={}", id);
        OrderResponse response = mapToResponse(findOrder(id));
        orderCache.put(id, response);
        return response;
    }

    @Transactional
    public OrderResponse createOrder(String idempotencyKey, OrderRequest request) {
        Optional<OrderEntity> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) { // bu anahtarla daha önce oluşmuş bir sipariş var mı
            return mapToResponse(existing.get());
        }
        OrderEntity order = new OrderEntity();
        order.setCustomerName(request.getCustomerName());
        order.setStatus(OrderStatus.PENDING);   // her sipariş beklemede başlar
        order.setCreatedAt(Instant.now());
        order.setIdempotencyKey(idempotencyKey);
        applyItems(order, request.getItems());

        // Stok sipariş anında düşer (rezerve edilir). Transaction rollback olursa stok otomatik geri verilir.
        applyStockChange(stockQuantities(order.getItems()));

        // saveAndFlush: kısıt ihlali (örn. aynı Idempotency-Key) commit'te değil burada patlasın, 409'a çevrilsin
        OrderEntity saved = orderRepository.saveAndFlush(order);
        pageCache.clear();              // sayfalar eskidi: yeni sipariş içlerinde yok
        return mapToResponse(saved);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        OrderEntity order = findOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(id,
                    "Sadece bekleyen sipariş güncellenebilir. Siparişin durumu: " + order.getStatus());
        }
        order.setCustomerName(request.getCustomerName());
        Map<Long, Integer> oldQuantities = stockQuantities(order.getItems());   // clear'dan önce al
        order.getItems().clear();   // orphanRemoval: eski kalemler silinir, yenileri eklenir
        applyItems(order, request.getItems());

        // Sadece fark kadar stok hareketi: (yeni adet - eski adet). Pozitif düşer, negatif iade edilir.
        applyStockChange(stockDifference(stockQuantities(order.getItems()), oldQuantities));

        OrderEntity saved = orderRepository.saveAndFlush(order);
        orderCache.remove(id);
        pageCache.clear();
        return mapToResponse(saved);
    }

    @Transactional
    public OrderResponse changeStatus(Long id, OrderStatus newStatus) {
        OrderEntity order = findOrder(id);
        if (!isTransitionAllowed(order.getStatus(), newStatus)) {
            throw new InvalidOrderStateException(id,
                    "Sipariş " + order.getStatus() + " durumundan " + newStatus + " durumuna geçirilemez.");
        }
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            // İptal: sipariş anında düşen stok geri verilir (negatif = iade)
            applyStockChange(negate(stockQuantities(order.getItems())));
        }

        OrderEntity saved = orderRepository.saveAndFlush(order);
        orderCache.remove(id);
        pageCache.clear();
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteOrder(Long id) {
        OrderEntity order = findOrder(id);
        // Kural: onaylanmış sipariş silinemez, önce iptal edilmesi gerekir
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(id, "Onaylanmış sipariş silinemez, önce iptal edin.");
        }
        // Bekleyen sipariş silinirse rezerve stok geri verilir. İptal edilmişin stoğu iptalde zaten iade edildi.
        if (order.getStatus() == OrderStatus.PENDING) {
            applyStockChange(negate(stockQuantities(order.getItems())));
        }

        orderRepository.delete(order);
        orderRepository.flush();
        orderCache.remove(id);
        pageCache.clear();
    }

    // Hangi durumdan hangisine geçilebilir. CANCELLED son durumdur.
    private boolean isTransitionAllowed(OrderStatus from, OrderStatus to) {
        if (from == OrderStatus.PENDING) {
            return to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
        } else if (from == OrderStatus.CONFIRMED) {
            return to == OrderStatus.CANCELLED;
        } else {
            return false;
        }
    }

    private OrderEntity findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir sipariş yok: " + id));
    }

    // İstekteki kalemleri siparişe ekler ve toplam tutarı (adet x birim fiyat) hesaplar.
    // Birim fiyat istemciden değil product-service'ten alınır ve siparişin anındaki haliyle kalemin üstüne yazılır
    // (kitabın fiyatı sonradan değişse de eski siparişin tutarı değişmez).
    private void applyItems(OrderEntity order, List<OrderItemRequest> requestItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest requestItem : requestItems) {
            BookDto book = productClient.getBook(requestItem.getBookId());
            OrderItemEntity item = new OrderItemEntity();
            item.setBookId(requestItem.getBookId());
            item.setQuantity(requestItem.getQuantity());
            item.setUnitPrice(book.getPrice());
            order.addItem(item);
            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(requestItem.getQuantity())));
        }
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    }

    // Stok haritası: kitap id -> değişim. Pozitif = stoktan düş, negatif = stoğa iade et.
    // Stok başka bir serviste/veritabanında olduğu için @Transactional onu kendiliğinden geri alamaz.
    // Bu yüzden yapılan stok hareketleri transaction'a kaydedilir; transaction rollback olursa
    // (metot içinde exception, commit hatası...) Spring revertStockChange'i çağırır.

    // Kalemlerin kitap başına toplam adedi (aynı kitap iki kalemde gelirse toplanır)
    private Map<Long, Integer> stockQuantities(List<OrderItemEntity> items) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (OrderItemEntity item : items) {
            result.merge(item.getBookId(), item.getQuantity(), Integer::sum);
        }
        return result;
    }

    private Map<Long, Integer> negate(Map<Long, Integer> quantities) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        quantities.forEach((bookId, quantity) -> result.put(bookId, -quantity));
        return result;
    }

    // yeni - eski; farkı sıfır olan kitaplar atlanır
    private Map<Long, Integer> stockDifference(Map<Long, Integer> newQuantities, Map<Long, Integer> oldQuantities) {
        Map<Long, Integer> result = new LinkedHashMap<>(newQuantities);
        oldQuantities.forEach((bookId, quantity) -> result.merge(bookId, -quantity, Integer::sum));
        result.values().removeIf(change -> change == 0);
        return result;
    }

    // Kitapları sırayla işler. Biri patlarsa (stok yetersiz, servis kapalı...) o ana kadar yapılanları hemen geri alıp
    // hatayı fırlatır. Hepsi başarılıysa, transaction rollback olursa geri alınmak üzere transaction'a kaydeder.
    private void applyStockChange(Map<Long, Integer> stockChange) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Stok değişimi aktif bir transaction içinde yapılmalı");
        }
        Map<Long, Integer> applied = new LinkedHashMap<>();
        try {
            for (Map.Entry<Long, Integer> entry : stockChange.entrySet()) {
                if (entry.getValue() > 0) {
                    productClient.decreaseStock(entry.getKey(), entry.getValue());
                } else if (entry.getValue() < 0) {
                    productClient.increaseStock(entry.getKey(), -entry.getValue());
                }
                applied.put(entry.getKey(), entry.getValue());
            }
        } catch (RuntimeException e) {
            revertStockChange(applied);
            throw e;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    revertStockChange(applied);
                }
            }
        });
    }

    // Telafi: yapılan stok hareketlerini tersine çevirir. Bu da patlarsa elle düzeltilebilsin diye loglanır, hata yutulur.
    private void revertStockChange(Map<Long, Integer> applied) {
        applied.forEach((bookId, change) -> {
            try {
                if (change > 0) {
                    productClient.increaseStock(bookId, change);
                } else if (change < 0) {
                    productClient.decreaseStock(bookId, -change);
                }
            } catch (RuntimeException ex) {
                log.atError()
                        .addKeyValue("eventCode", "STOCK_COMPENSATION_FAILED")
                        .addKeyValue("bookId", bookId)
                        .addKeyValue("change", change)
                        .setCause(ex)
                        .log("Stok telafisi yapılamadı, stok elle düzeltilmeli");
            }
        });
    }

    private OrderResponse mapToResponse(OrderEntity entity) {
        OrderResponse response = new OrderResponse();
        response.setId(entity.getId());
        response.setCustomerName(entity.getCustomerName());
        response.setStatus(entity.getStatus());
        response.setTotalAmount(entity.getTotalAmount());
        response.setCreatedAt(entity.getCreatedAt());
        response.setItems(entity.getItems().stream().map(this::mapToItemResponse).toList());
        return response;
    }

    private OrderItemResponse mapToItemResponse(OrderItemEntity entity) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(entity.getId());
        response.setBookId(entity.getBookId());
        response.setQuantity(entity.getQuantity());
        response.setUnitPrice(entity.getUnitPrice());
        return response;
    }
}
