package com.example.orderservice.api.service;

import com.example.orderservice.api.exception.InvalidOrderStateException;
import com.example.orderservice.api.model.OrderEntity;
import com.example.orderservice.api.model.OrderItemEntity;
import com.example.orderservice.api.model.OrderStatus;
import com.example.orderservice.api.repository.OrderRepository;
import com.example.orderservice.api.request.OrderItemRequest;
import com.example.orderservice.api.request.OrderRequest;
import com.example.orderservice.api.response.OrderItemResponse;
import com.example.orderservice.api.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional(readOnly = true)   // Okuma metotları: oturum açık kalır, LAZY kalemlere güvenle erişilir
public class OrderService {
    // Hangi durumdan hangisine geçilebilir. CANCELLED son durumdur.
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.CANCELLED),
            OrderStatus.CANCELLED, Set.of()
    );

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // status verilirse sadece o durumdaki siparişler gelir
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<OrderEntity> page = (status == null)
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
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

        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        OrderEntity order = findOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(id,
                    "Sadece bekleyen sipariş güncellenebilir. Siparişin durumu: " + order.getStatus());
        }
        order.setCustomerName(request.getCustomerName());
        order.getItems().clear();   // orphanRemoval: eski kalemler silinir, yenileri eklenir
        applyItems(order, request.getItems());

        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse changeStatus(Long id, OrderStatus newStatus) {
        OrderEntity order = findOrder(id);
        if (!ALLOWED_TRANSITIONS.get(order.getStatus()).contains(newStatus)) {
            throw new InvalidOrderStateException(id,
                    "Sipariş " + order.getStatus() + " durumundan " + newStatus + " durumuna geçirilemez.");
        }
        order.setStatus(newStatus);
        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrder(Long id) {
        OrderEntity order = findOrder(id);
        // Kural: onaylanmış sipariş silinemez, önce iptal edilmesi gerekir
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(id, "Onaylanmış sipariş silinemez, önce iptal edin.");
        }
        orderRepository.delete(order);
    }

    private OrderEntity findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir sipariş yok: " + id));
    }

    // İstekteki kalemleri siparişe ekler ve toplam tutarı (adet x birim fiyat) hesaplar
    private void applyItems(OrderEntity order, List<OrderItemRequest> requestItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest requestItem : requestItems) {
            OrderItemEntity item = new OrderItemEntity();
            item.setBookId(requestItem.getBookId());
            item.setQuantity(requestItem.getQuantity());
            item.setUnitPrice(requestItem.getUnitPrice());
            order.addItem(item);
            total = total.add(requestItem.getUnitPrice().multiply(BigDecimal.valueOf(requestItem.getQuantity())));
        }
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
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
