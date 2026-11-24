package com.example.NativeProject.api.service;

import com.example.NativeProject.api.exception.AuthorHasBooksException;
import com.example.NativeProject.api.model.AuthorEntity;
import com.example.NativeProject.api.repository.AuthorRepository;
import com.example.NativeProject.api.repository.BookRepository;
import com.example.NativeProject.api.request.AuthorRequest;
import com.example.NativeProject.api.response.AuthorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
public class AuthorService {
    private static final Logger log = LoggerFactory.getLogger(AuthorService.class);
    // Her farklı page/size/sort yeni bir anahtar açar; bellek şişmesin diye sınır koyuyoruz
    private static final int MAX_PAGE_CACHE_SIZE = 100;

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;

    // Cache 1: yazar id -> yazar (DTO tutuyoruz, entity değil)
    private final Map<Long, AuthorResponse> authorCache = new ConcurrentHashMap<>();
    // Cache 2: sayfa isteği (page, size, sort) -> o sayfanın sonucu
    private final Map<Pageable, Page<AuthorResponse>> pageCache = new ConcurrentHashMap<>();

    public AuthorService(AuthorRepository authorRepository,
                         BookRepository bookRepository,
                         BookService bookService) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
    }

    public Page<AuthorResponse> getAllAuthors(Pageable pageable) {
        Page<AuthorResponse> cached = pageCache.get(pageable);
        if (cached != null) {
            log.info("CACHE HIT: yazar sayfası {}", pageable);
            return cached;
        }
        log.info("CACHE MISS: yazar sayfası {}", pageable);
        Page<AuthorResponse> page = authorRepository.findAll(pageable)
                .map(this::mapToResponse);
        if (pageCache.size() >= MAX_PAGE_CACHE_SIZE) {
            pageCache.clear();          // basit strateji: dolunca hepsini boşalt
        }
        pageCache.put(pageable, page);
        return page;
    }

    public AuthorResponse getAuthorById(Long id) {
        AuthorResponse cached = authorCache.get(id);
        if (cached != null) {
            log.info("CACHE HIT: yazar id={}", id);
            return cached;
        }
        log.info("CACHE MISS: yazar id={}", id);
        AuthorEntity author = authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir yazar yok: " + id));
        AuthorResponse response = mapToResponse(author);
        authorCache.put(id, response);
        return response;
    }

    @Transactional
    public AuthorResponse createAuthor(AuthorRequest request) {
        AuthorEntity author = new AuthorEntity();
        author.setName(request.getName());
        author.setBirthYear(request.getBirthYear());

        AuthorEntity saved = authorRepository.save(author);
        pageCache.clear();              // sayfalar eskidi: yeni yazar içlerinde yok
        return mapToResponse(saved);
    }

    @Transactional
    public AuthorResponse updateAuthor(Long id, AuthorRequest request) {
        AuthorEntity existingAuthor = authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir yazar yok: " + id));
        existingAuthor.setName(request.getName());
        existingAuthor.setBirthYear(request.getBirthYear());

        AuthorEntity saved = authorRepository.save(existingAuthor);
        authorCache.remove(id);
        pageCache.clear();
        bookService.clearAllCache();   // kitap cache'inde eski yazar adı kalmasın
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
        authorCache.remove(id);
        pageCache.clear();
        // Kitap cache'ine dokunmaya gerek yok: kitabı olan yazar zaten silinemiyor
    }

    private AuthorResponse mapToResponse(AuthorEntity entity) {
        AuthorResponse response = new AuthorResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setBirthYear(entity.getBirthYear());
        return response;
    }
}
