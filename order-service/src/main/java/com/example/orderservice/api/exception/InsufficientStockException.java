package com.example.orderservice.api.exception;

// product-service stok düşmeyi reddetti (409): istenen adet mevcut stoktan fazla
public class InsufficientStockException extends RuntimeException {
    private final Long bookId;

    public InsufficientStockException(Long bookId) {
        super("Stok yetersiz: kitap " + bookId);
        this.bookId = bookId;
    }

    public Long getBookId() {
        return bookId;
    }
}
