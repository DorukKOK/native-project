package com.example.NativeProject.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockRequest {
    @NotNull(message = "Miktar bos olamaz.")
    @Min(value = 0,message = "Stok en az 0 olmalidir")
    private Integer quantity;

}
