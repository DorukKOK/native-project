package com.example.NativeProject.api.config;

import com.example.NativeProject.api.request.AuthorRequest;
import com.example.NativeProject.api.request.BookRequest;
import com.example.NativeProject.api.response.AuthorResponse;
import com.example.NativeProject.api.response.BookResponse;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

// Native image: JSON'a çevrilen sınıfları (Jackson reflection ile okur) derleyiciye önceden bildiriyoruz.
@Configuration
@RegisterReflectionForBinding({
        AuthorRequest.class, AuthorResponse.class,
        BookRequest.class, BookResponse.class
})
public class NativeHintsConfig {
}