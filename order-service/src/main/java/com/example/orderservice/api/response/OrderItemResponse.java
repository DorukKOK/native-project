package com.example.orderservice.api.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponse {
    private Long id;
    private Long bookId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
