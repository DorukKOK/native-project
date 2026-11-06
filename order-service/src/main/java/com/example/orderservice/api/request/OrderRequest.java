package com.example.orderservice.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {
    @NotBlank(message = "Musteri adi bos olamaz.")
    private String customerName;

    @NotEmpty(message = "Siparis en az bir kalem icermelidir.")
    // @Valid: listedeki her kalemin kendi kurallari da kontrol edilsin; @NotNull: listede bos (null) kalem olmasin
    private List<@Valid @NotNull(message = "Kalem bos olamaz.") OrderItemRequest> items;
}
