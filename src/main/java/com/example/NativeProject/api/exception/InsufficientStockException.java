package com.example.NativeProject.api.exception;

public class InsufficientStockException extends RuntimeException {
    //Stok hep kitap idsiyle çağrılır o yüzden stock yerine bookId
    private final Long bookId;
    public InsufficientStockException(Long bookId) {
        super("Kitap " + bookId + " icin yeterli stok yok.");
        this.bookId = bookId;
    }

    public Long getBookId() {return bookId;}
}
