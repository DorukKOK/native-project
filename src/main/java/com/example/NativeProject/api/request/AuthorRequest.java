package com.example.NativeProject.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorRequest {
    @NotBlank(message = "yazar adi bos olamaz.")
    private String name;
    @PositiveOrZero(message = "Doğum yılı negatif olamaz.")
    private Integer birthYear;
}
