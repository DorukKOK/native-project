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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
@Service
@Transactional(readOnly = true)   // Okuma metotları: oturum açık kalır, LAZY kalemlere güvenle erişilir
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    // status verilirse sadece o durumdaki siparişler gelir
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<OrderEntity> page;
        if (status == null) {
            page = orderRepository.findAll(pageable);
        } else {
            page = orderRepository.findByStatus(status, pageable);
        }
        return page.map(this::mapToResponse);
    }

    public OrderResponse getOrderById(Long id) {
        return mapToResponse(findOrder(id));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        OrderEntity order = new OrderEntity();
        order.setCustomerName(request.getCustomerName());
        order.setStatus(OrderStatus.PENDING);   // her sipariş beklemede başlar
        order.setCreatedAt(Instant.now());
        applyItems(order, request.getItems());

        // Stok sipariş anında düşer (rezerve edilir); kayıt patlarsa stok geri verilir
        Map<Long, Integer> stockChange = stockQuantities(order.getItems());
        return mapToResponse(changeStockThenSave(stockChange, () -> orderRepository.saveAndFlush(order)));
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
        Map<Long, Integer> stockChange = stockDifference(stockQuantities(order.getItems()), oldQuantities);
        return mapToResponse(changeStockThenSave(stockChange, () -> orderRepository.saveAndFlush(order)));
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
            Map<Long, Integer> stockChange = negate(stockQuantities(order.getItems()));
            return mapToResponse(changeStockThenSave(stockChange, () -> orderRepository.saveAndFlush(order)));
        }
        return mapToResponse(orderRepository.save(order));
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
            Map<Long, Integer> stockChange = negate(stockQuantities(order.getItems()));
            changeStockThenSave(stockChange, () -> {
                orderRepository.delete(order);
                orderRepository.flush();
                return null;
            });
        } else {
            orderRepository.delete(order);
        }
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

    // ---- Stok yönetimi (product-service ile) ----
    // Stok haritası: kitap id -> değişim. Pozitif = stoktan düş, negatif = stoğa iade et.

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

    // Önce stoğu değiştirir, sonra veritabanı yazımını yapar. Yazım patlarsa stok değişimi geri alınır.
    private <T> T changeStockThenSave(Map<Long, Integer> stockChange, Supplier<T> databaseWrite) {
        Map<Long, Integer> applied = applyStockChange(stockChange);
        try {
            return databaseWrite.get();
        } catch (RuntimeException e) {
            revertStockChange(applied);
            throw e;
        }
    }

    // Kitapları sırayla işler. Biri patlarsa (stok yetersiz, servis kapalı...) o ana kadar yapılanları geri alıp hatayı fırlatır.
    private Map<Long, Integer> applyStockChange(Map<Long, Integer> stockChange) {
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
        return applied;
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
