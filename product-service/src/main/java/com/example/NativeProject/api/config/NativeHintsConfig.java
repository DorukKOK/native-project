package com.example.NativeProject.api.config;

import com.example.NativeProject.api.request.AuthorRequest;
import com.example.NativeProject.api.request.BookRequest;
import com.example.NativeProject.api.request.StockChangeRequest;
import com.example.NativeProject.api.request.StockRequest;
import com.example.NativeProject.api.response.AuthorResponse;
import com.example.NativeProject.api.response.BookResponse;
import com.example.NativeProject.api.response.StockResponse;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PagedModel;

// Native image: JSON'a çevrilen sınıfları (Jackson reflection ile okur) derleyiciye önceden bildiriyoruz.
// Tek yerde tutuyoruz; controller'lara tekrar yazmaya gerek yok.
@Configuration
@RegisterReflectionForBinding({
        AuthorRequest.class, AuthorResponse.class,
        BookRequest.class, BookResponse.class,
        StockRequest.class, StockResponse.class,
        StockChangeRequest.class,
        PagedModel.class, PagedModel.PageMetadata.class   // sayfalı cevapların gövdesi ve "page" bilgisi
})
public class NativeHintsConfig {
}
