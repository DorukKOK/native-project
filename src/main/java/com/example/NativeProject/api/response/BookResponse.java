package com.example.NativeProject.api.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BookResponse {
    private Long id;
    private String title;
    private Integer pageCount;
    private BigDecimal price;
    private Long authorId;
    private String authorName;
}
