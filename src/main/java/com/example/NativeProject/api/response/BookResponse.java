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
    private Integer quantity;

    // Cache'teki nesneye dokunmadan, güncel stok yazılmış bir kopya üretir
    public BookResponse copyWithQuantity(Integer quantity) {
        BookResponse copy = new BookResponse();
        copy.setId(id);
        copy.setTitle(title);
        copy.setPageCount(pageCount);
        copy.setPrice(price);
        copy.setAuthorId(authorId);
        copy.setAuthorName(authorName);
        copy.setQuantity(quantity);
        return copy;
    }
}
