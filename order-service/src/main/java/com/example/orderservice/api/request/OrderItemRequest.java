package com.example.orderservice.api.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemRequest {
    @NotNull(message = "Kitap id bos olamaz.")
    @Positive(message = "Kitap id pozitif olmalidir.")
    private Long bookId;

    @NotNull(message = "Adet bos olamaz.")
    @Min(value = 1, message = "Adet en az 1 olmalidir.")
    private Integer quantity;

    // Şimdilik istemciden gelir; product-service ile haberleşme yazılınca kitabın fiyatından doldurulacak
    // @Digits: veritabanı sütunu NUMERIC(10,2). Fazla ondalık gelirse DB yuvarlar ama toplam yuvarlanmamış
    // fiyattan hesaplanırdı; kalem toplamıyla sipariş toplamı tutmazdı. Bu yüzden baştan reddediyoruz.
    @NotNull(message = "Birim fiyat bos olamaz.")
    @PositiveOrZero(message = "Birim fiyat negatif olamaz.")
    @Digits(integer = 8, fraction = 2, message = "Birim fiyat en fazla 8 tam ve 2 ondalik basamak icerebilir.")
    private BigDecimal unitPrice;
}
