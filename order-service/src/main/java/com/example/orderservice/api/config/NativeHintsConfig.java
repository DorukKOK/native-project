package com.example.orderservice.api.config;

import com.example.orderservice.api.client.BookDto;
import com.example.orderservice.api.request.OrderItemRequest;
import com.example.orderservice.api.request.OrderRequest;
import com.example.orderservice.api.request.OrderStatusRequest;
import com.example.orderservice.api.response.OrderItemResponse;
import com.example.orderservice.api.response.OrderResponse;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PagedModel;

// Native image: JSON'a çevrilen sınıfları (Jackson reflection ile okur) derleyiciye önceden bildiriyoruz.
@Configuration
@RegisterReflectionForBinding({
        OrderRequest.class, OrderItemRequest.class, OrderStatusRequest.class,
        OrderResponse.class, OrderItemResponse.class,
        BookDto.class,                                    // product-service cevabı (JSON'dan okunur)
        PagedModel.class, PagedModel.PageMetadata.class   // sayfalı cevapların gövdesi ve "page" bilgisi
})
public class NativeHintsConfig {
}
