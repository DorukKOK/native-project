package com.example.NativeProject.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockChangeRequest {

    @NotNull(message = "Miktar bos olamaz.")
    @Min(value = 1, message = "Miktar en az 1 olmalidir.")
    private Integer amount;
}