package com.example.NativeProject.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BookRequest {
    @NotBlank(message = "Kitap adı bos olamaz.")
    private String title;

    private Integer pageCount;
    private BigDecimal price;
    private Long authorId;
}