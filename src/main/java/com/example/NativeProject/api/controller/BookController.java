package com.example.NativeProject.api.controller;

import com.example.NativeProject.api.request.BookRequest;
import com.example.NativeProject.api.request.StockChangeRequest;
import com.example.NativeProject.api.request.StockRequest;
import com.example.NativeProject.api.response.BookResponse;
import com.example.NativeProject.api.response.StockResponse;
import com.example.NativeProject.api.service.BookService;
import com.example.NativeProject.api.service.StockService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {
    private final BookService bookService;
    private final StockService stockService;

    public BookController(BookService bookService, StockService stockService) {
        this.bookService = bookService;
        this.stockService = stockService;
    }

    // GET /books?page=0&size=10&sort=title,asc
    @GetMapping
    public PagedModel<BookResponse> getAllBooks(Pageable pageable) {
        return new PagedModel<>(bookService.getAllBooks(pageable));
    }

    @GetMapping("/{id}")
    public BookResponse getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @PutMapping("/{id}")
    public BookResponse updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return bookService.updateBook(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{bookId}/stock")
    public StockResponse getStocksByBookId(@PathVariable Long bookId){
        return stockService.getStockByBookId(bookId);
    }
    @PostMapping("/{bookId}/stock/increase")
    public ResponseEntity<StockResponse> increaseStock(@PathVariable Long bookId,
                                                       @Valid @RequestBody StockChangeRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(stockService.increaseStock(bookId,request));
    }
    @PostMapping("/{bookId}/stock/decrease")
    public ResponseEntity<StockResponse> decreaseStock(@PathVariable Long bookId,
                                                       @Valid @RequestBody StockChangeRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(stockService.decreaseStock(bookId,request));
    }
    @PutMapping("/{bookId}/stock")
    public StockResponse updateStock(@PathVariable Long bookId,
                                    @Valid @RequestBody StockRequest request){
        return stockService.updateStock(bookId,request);
    }


}