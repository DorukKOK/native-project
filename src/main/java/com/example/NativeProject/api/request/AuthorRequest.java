package com.example.NativeProject.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorRequest {
    @NotBlank(message = "yazar adi bos olamaz.")
    private String name;
    private Integer birthYear;
}
