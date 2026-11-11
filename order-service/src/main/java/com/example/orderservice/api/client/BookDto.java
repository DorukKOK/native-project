package com.example.orderservice.api.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// product-service'in BookResponse'unun order-service'teki kendi kopyası.
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)   // title, authorName, quantity vs. gelse de yok say(bilmediğin alanı görmezden gel)
public class BookDto {
    private Long id;
    private BigDecimal price;
    private Integer quantity;
}
