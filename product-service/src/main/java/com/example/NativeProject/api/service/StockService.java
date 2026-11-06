package com.example.NativeProject.api.service;

import com.example.NativeProject.api.exception.InsufficientStockException;
import com.example.NativeProject.api.repository.BookRepository;
import com.example.NativeProject.api.request.StockChangeRequest;
import com.example.NativeProject.api.request.StockRequest;
import com.example.NativeProject.api.response.StockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.NoSuchElementException;

// Stok artık books tablosunda (stock_quantity). Değişiklikler BookRepository'deki atomik UPDATE'lerle yapılır.
@Service
@RequiredArgsConstructor
public class StockService {
    private final BookRepository bookRepository;
    private final BookService bookService;

    @Transactional(readOnly = true)
    public StockResponse getStockByBookId(Long bookId) {
        Integer quantity = bookRepository.findStockQuantityById(bookId)
                .orElseThrow(() -> bookNotFound(bookId));
        return toResponse(bookId, quantity);
    }

    @Transactional
    public StockResponse updateStock(Long bookId, StockRequest request) {
        if (bookRepository.setStock(bookId, request.getQuantity()) == 0) {
            throw bookNotFound(bookId);
        }
        clearBookCacheAfterCommit(bookId);
        return getStockByBookId(bookId);
    }

    @Transactional
    public StockResponse increaseStock(Long bookId, StockChangeRequest request) {
        if (bookRepository.increaseStock(bookId, request.getAmount()) == 0) {
            throw bookNotFound(bookId);
        }
        clearBookCacheAfterCommit(bookId);
        return getStockByBookId(bookId);
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
        clearBookCacheAfterCommit(bookId);
        return getStockByBookId(bookId);
    }

    // Cache'i işlem commit olduktan sonra temizliyoruz: önce temizlersek, o arada gelen bir okuma
    // henüz commit olmamış eski stoğu cache'e geri yazabilir.
    private void clearBookCacheAfterCommit(Long bookId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                bookService.clearCache(bookId);
            }
        });
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
