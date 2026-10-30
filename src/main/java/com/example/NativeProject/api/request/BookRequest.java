package com.example.NativeProject.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BookRequest {
    @NotBlank(message = "Kitap adı bos olamaz.")
    private String title;

    @PositiveOrZero(message = "Sayfa sayısı negatif olamaz.")
    private Integer pageCount;

    @PositiveOrZero(message = "Fiyat negatif olamaz.")
    private BigDecimal price;
    // authorId burada yok: yazar, URL'den gelir (POST /authors/{authorId}/books)
}