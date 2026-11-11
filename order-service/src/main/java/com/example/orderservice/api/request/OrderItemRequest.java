package com.example.orderservice.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {
    @NotNull(message = "Kitap id bos olamaz.")
    @Positive(message = "Kitap id pozitif olmalidir.")
    private Long bookId;

    @NotNull(message = "Adet bos olamaz.")
    @Min(value = 1, message = "Adet en az 1 olmalidir.")
    private Integer quantity;
}
