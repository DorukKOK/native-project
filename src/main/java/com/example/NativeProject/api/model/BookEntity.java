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

    @ManyToOne(fetch = FetchType.LAZY) //gerçekten ihtiyacın olana kadar veriyi getirme

    @JoinColumn(name = "author_id")
    private AuthorEntity authorEntity;
}
