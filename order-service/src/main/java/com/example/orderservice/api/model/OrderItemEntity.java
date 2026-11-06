package com.example.orderservice.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity orderEntity;

    // product-service'teki kitabın id'si. İlişki (@ManyToOne) değil düz sayı: kitap başka serviste.
    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private Integer quantity;

    // Sipariş anındaki birim fiyat; kitabın fiyatı sonradan değişse de siparişin tutarı değişmesin
    @Column(nullable = false)
    private BigDecimal unitPrice;
}
