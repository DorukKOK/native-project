package com.example.api.service;

import com.example.api.exception.InsufficientStockException;
import com.example.api.repository.BookRepository;
import com.example.api.request.StockChangeRequest;
import com.example.api.request.StockRequest;
import com.example.api.response.StockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

// Stok artık books tablosunda (stock_quantity). Değişiklikler BookRepository'deki atomik UPDATE'lerle yapılır.
// Stok için ayrı bir cache tutulmuyor: stok zaten BookService'in kitap cache'indeki BookResponse.quantity alanında.
// Ayrı bir stockCache olsaydı kitap silinince/güncellenince onu da temizlemek gerekirdi; BookService -> StockService
// bağımlılığı da döngüsel bağımlılık (StockService zaten BookService'e bağlı) yaratırdı.
@Service
@RequiredArgsConstructor
public class StockService {
    private final BookRepository bookRepository;
    private final BookService bookService;

    // Okuma cache'ten: BookService.getBookById (CACHE HIT/MISS orada loglanır, kitap yoksa 404)
    @Transactional(readOnly = true)
    public StockResponse getStockByBookId(Long bookId) {
        return toResponse(bookId, bookService.getBookById(bookId).getQuantity());
    }

    @Transactional
    public StockResponse updateStock(Long bookId, StockRequest request) {
        if (bookRepository.setStock(bookId, request.getQuantity()) == 0) {
            throw bookNotFound(bookId);
        }
        // Normal kullanım: transaction İÇİNDE, commit'ten ÖNCE temizler
        bookService.evictBook(bookId);
        return readStockFromDb(bookId);
    }

    @Transactional
    public StockResponse increaseStock(Long bookId, StockChangeRequest request) {
        if (bookRepository.increaseStock(bookId, request.getAmount()) == 0) {
            throw bookNotFound(bookId);
        }
        // Normal kullanım: transaction İÇİNDE, commit'ten ÖNCE temizler
        bookService.evictBook(bookId);
        return readStockFromDb(bookId);
    }

    @Transactional
    public StockResponse decreaseStock(Long bookId, StockChangeRequest request) {
        if (bookRepository.decreaseStock(bookId, request.getAmount()) == 0) {
            // Güncelleme etkisizse iki sebep olabilir: kitap yok (404) ya da stok yetersiz (409)
            if (!bookRepository.existsById(bookId)) {
                throw bookNotFound(bookId);
            }
            throw new InsufficientStockException(bookId);
        }
        // Normal kullanım: transaction İÇİNDE, commit'ten ÖNCE temizler
        bookService.evictBook(bookId);
        return readStockFromDb(bookId);
    }

    // Yazma sonrası cevap DB'den okunur, cache'ten DEĞİL: getBookById burada çağrılsaydı henüz commit olmamış
    // yeni stoğu cache'e koyardı; transaction rollback olursa cache'te hiç gerçekleşmemiş bir stok kalırdı.
    private StockResponse readStockFromDb(Long bookId) {
        Integer quantity = bookRepository.findStockQuantityById(bookId)
                .orElseThrow(() -> bookNotFound(bookId));
        return toResponse(bookId, quantity);
    }

    private NoSuchElementException bookNotFound(Long bookId) {
        return new NoSuchElementException("Böyle bir kitap yok: " + bookId);
    }

    private StockResponse toResponse(Long bookId, Integer quantity) {
        StockResponse response = new StockResponse();
        response.setBookId(bookId);
        response.setQuantity(quantity);
        return response;
    }
}
