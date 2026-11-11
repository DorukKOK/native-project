package com.example.orderservice.api.client;

import com.example.orderservice.api.exception.InsufficientStockException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.NoSuchElementException;

// product-service ile konuşan tek yer. OrderService HTTP detayını bilmez, sadece bu sınıfı çağırır.
@Component //Bu sınıfın kendisinden bir nesne üret ve sakla.
public class ProductClient {
    private final RestClient restClient;

    // Aynı tipte tek bean olduğu için Spring RestClientConfig.productRestClient'ı kendisi enjekte eder
    public ProductClient(RestClient restClient) {
        this.restClient = restClient;
    }

    // GET /books/{id}. Kitap yoksa NoSuchElementException (404'e çevrilir).
    // Bağlantı/timeout hatası (ResourceAccessException) bilerek yakalanmıyor: GlobalExceptionHandler 503'e çevirecek.
    public BookDto getBook(Long bookId) {
        try {
            BookDto book = restClient.get()
                    .uri("/books/{id}", bookId)
                    .retrieve()
                    .body(BookDto.class); //cevabın gövdesi nesneye çevrilir
            if (book == null || book.getPrice() == null) {
                throw new IllegalStateException("product-service kitap " + bookId + " için geçerli bir cevap dönmedi");
            }
            return book;
        } catch (HttpClientErrorException.NotFound e) {
            throw new NoSuchElementException("Böyle bir kitap yok: " + bookId);
        }
    }

    // POST /books/{id}/stock/decrease. Stok yetersizse (409) InsufficientStockException, kitap yoksa NoSuchElementException.
    public void decreaseStock(Long bookId, int amount) {
        try {
            postStockChange(bookId, "decrease", amount);
        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientStockException(bookId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NoSuchElementException("Böyle bir kitap yok: " + bookId);
        }
    }

    // POST /books/{id}/stock/increase (iptal / telafi için stok iadesi)
    public void increaseStock(Long bookId, int amount) {
        try {
            postStockChange(bookId, "increase", amount);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NoSuchElementException("Böyle bir kitap yok: " + bookId);
        }
    }

    private void postStockChange(Long bookId, String action, int amount) {
        restClient.post()
                .uri("/books/{id}/stock/{action}", bookId, action)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .toBodilessEntity();
    }
}
