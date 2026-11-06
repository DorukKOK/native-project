package com.example.orderservice.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // @BatchSize: sayfadaki siparişlerin kalemleri tek sorguda gelir, sipariş başına ayrı sorgu atılmaz (N+1 yok)
    @OneToMany(mappedBy = "orderEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItemEntity> items = new ArrayList<>();

    // İki yönlü ilişkiyi tek yerden kuruyoruz: kalemi listeye ekler ve kalemin siparişini ayarlar
    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrderEntity(this);
    }
}
