package com.example.NativeProject.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "books")
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private Integer pageCount;
    private BigDecimal price;

    // updatable = false: kitap bilgisi (başlık/fiyat) güncellenirken stok sütunu UPDATE'e girmez,
    // yoksa eşzamanlı bir stok değişikliği eski değerle ezilebilir. Stok yalnızca BookRepository'deki
    // atomik sorgularla değişir.
    @Column(name = "stock_quantity", nullable = false, updatable = false)
    private Integer stockQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY) //gerçekten ihtiyacın olana kadar veriyi getirme

    @JoinColumn(name = "author_id")
    private AuthorEntity authorEntity;
}
