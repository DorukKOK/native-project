package com.example.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "stock_operations")
public class StockOperationsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "idempotency_key",nullable = false)
    private String idempotencyKey;

    @Column(name = "book_id",nullable = false)
    private Long book_id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type",nullable = false)
    private StockOperationType operationType;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
