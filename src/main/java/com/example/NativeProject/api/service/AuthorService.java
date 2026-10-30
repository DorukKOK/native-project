package com.example.NativeProject.api.service;

import com.example.NativeProject.api.exception.AuthorHasBooksException;
import com.example.NativeProject.api.model.AuthorEntity;
import com.example.NativeProject.api.repository.AuthorRepository;
import com.example.NativeProject.api.repository.BookRepository;
import com.example.NativeProject.api.request.AuthorRequest;
import com.example.NativeProject.api.response.AuthorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;

    public AuthorService(AuthorRepository authorRepository,
                         BookRepository bookRepository,
                         BookService bookService) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
    }

    public Page<AuthorResponse> getAllAuthors(Pageable pageable) {
        return authorRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public AuthorResponse getAuthorById(Long id) {
        AuthorEntity author = authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir yazar yok: " + id));
        return mapToResponse(author);
    }

    @Transactional
    public AuthorResponse createAuthor(AuthorRequest request) {
        AuthorEntity author = new AuthorEntity();
        author.setName(request.getName());
        author.setBirthYear(request.getBirthYear());

        AuthorEntity saved = authorRepository.save(author);
        return mapToResponse(saved);
    }

    @Transactional
    public AuthorResponse updateAuthor(Long id, AuthorRequest request) {
        AuthorEntity existingAuthor = authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir yazar yok: " + id));
        existingAuthor.setName(request.getName());
        existingAuthor.setBirthYear(request.getBirthYear());

        AuthorEntity saved = authorRepository.save(existingAuthor);
        bookService.clearCache();   // kitap cache'inde eski yazar adı kalmasın
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new NoSuchElementException("Böyle bir yazar yok: " + id);
        }
        // Kural: kitabı olan yazar silinemez (DB'deki ON DELETE RESTRICT son güvenlik ağı)
        if (bookRepository.existsByAuthorEntityId(id)) {
            throw new AuthorHasBooksException(id);
        }
        authorRepository.deleteById(id);
    }

    private AuthorResponse mapToResponse(AuthorEntity entity) {
        AuthorResponse response = new AuthorResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setBirthYear(entity.getBirthYear());
        return response;
    }
}