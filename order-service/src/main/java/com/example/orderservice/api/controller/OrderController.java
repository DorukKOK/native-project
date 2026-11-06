package com.example.orderservice.api.controller;

import com.example.orderservice.api.model.OrderStatus;
import com.example.orderservice.api.request.OrderRequest;
import com.example.orderservice.api.request.OrderStatusRequest;
import com.example.orderservice.api.response.OrderResponse;
import com.example.orderservice.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // GET /orders?page=0&size=10&sort=createdAt,desc&status=PENDING   (status isteğe bağlı)
    @GetMapping
    public PagedModel<OrderResponse> getAllOrders(@RequestParam(required = false) OrderStatus status,
                                                  Pageable pageable) {
        return new PagedModel<>(orderService.getAllOrders(status, pageable));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    // Sadece bekleyen (PENDING) sipariş güncellenebilir
    @PutMapping("/{id}")
    public OrderResponse updateOrder(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return orderService.updateOrder(id, request);
    }

    // PUT /orders/5/status  {"status":"CONFIRMED"}
    @PutMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return orderService.changeStatus(id, request.getStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
