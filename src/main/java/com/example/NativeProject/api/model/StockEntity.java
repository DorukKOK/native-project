package com.example.NativeProject.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity
@Getter
@Setter
@Table(name = "stocks")
public class StockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Integer quantity;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id",nullable = false,unique = true)
    private BookEntity bookEntity;
}
