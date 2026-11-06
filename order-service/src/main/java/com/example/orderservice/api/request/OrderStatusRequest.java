package com.example.orderservice.api.request;

import com.example.orderservice.api.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusRequest {
    @NotNull(message = "Durum bos olamaz.")
    private OrderStatus status;
}
