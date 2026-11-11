package com.example.orderservice.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

// product-service'e giden HTTP isteklerini atan istemci. Adres application.properties'ten gelir.
@Configuration //bu sınıfta bean üreten method var.
public class RestClientConfig {

    // Bağlantı kurulamazsa 2 sn'de vazgeç; bağlanıp cevap gelmezse 5 sn'de vazgeç.
    // Timeout olmazsa product-service takıldığında sipariş isteği sonsuza kadar bekler.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    RestClient productRestClient(@Value("${product-service.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder() //boş bir ayar formu açar
                .baseUrl(baseUrl) //formun adres kısmını doldurur
                .requestFactory(requestFactory) //isteği gerçekten gönderecek motoru seçer(Javanın kendi HTTP Clienti)
                .build();
    }
}
