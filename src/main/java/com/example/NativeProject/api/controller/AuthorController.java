package com.example.NativeProject.api.controller;

import com.example.NativeProject.api.request.AuthorRequest;
import com.example.NativeProject.api.request.BookRequest;
import com.example.NativeProject.api.response.AuthorResponse;
import com.example.NativeProject.api.response.BookResponse;
import com.example.NativeProject.api.service.AuthorService;
import com.example.NativeProject.api.service.BookService;
import jakarta.validation.Valid;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Configuration
@RegisterReflectionForBinding({
        AuthorRequest.class, AuthorResponse.class,
        BookRequest.class, BookResponse.class
})
@RequestMapping("/authors")
public class AuthorController {
    private final AuthorService authorService;
    private final BookService bookService;

    public AuthorController(AuthorService authorService, BookService bookService) {
        this.authorService = authorService;
        this.bookService = bookService;
    }
    
    @GetMapping
    public PagedModel<AuthorResponse> getAllAuthors(Pageable pageable) {
        return new PagedModel<>(authorService.getAllAuthors(pageable));
    }

    @GetMapping("/{id}")
    public AuthorResponse getAuthorById(@PathVariable Long id) {
        return authorService.getAuthorById(id);
    }

    @PostMapping
    public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authorService.createAuthor(request));
    }

    @PutMapping("/{id}")
    public AuthorResponse updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
        return authorService.updateAuthor(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }

    // --- Bir yazarın kitapları ---

    // GET /authors/5/books?page=0&size=10
    @GetMapping("/{authorId}/books")
    public PagedModel<BookResponse> getBooksByAuthor(@PathVariable Long authorId, Pageable pageable) {
        return new PagedModel<>(bookService.getBooksByAuthor(authorId, pageable));
    }

    // POST /authors/5/books  -> yazar id URL'den gelir
    @PostMapping("/{authorId}/books")
    public ResponseEntity<BookResponse> createBook(@PathVariable Long authorId,
                                                   @Valid @RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request, authorId));
    }
}