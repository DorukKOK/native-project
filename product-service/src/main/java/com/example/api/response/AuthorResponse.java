package com.example.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorResponse {
    private Long id;
    private String name;
    private Integer birthYear;
}
