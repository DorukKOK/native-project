package com.example.NativeProject.api.service;

import com.example.NativeProject.api.model.AuthorEntity;
import com.example.NativeProject.api.model.BookEntity;
import com.example.NativeProject.api.repository.AuthorRepository;
import com.example.NativeProject.api.repository.BookRepository;
import com.example.NativeProject.api.request.BookRequest;
import com.example.NativeProject.api.response.BookResponse;
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
@Transactional(readOnly = true)   // Okuma metotları: oturum açık kalır, LAZY yazara güvenle erişilir
public class BookService {
    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    // Her farklı page/size/sort yeni bir anahtar açar; bellek şişmesin diye sınır koyuyoruz
    private static final int MAX_PAGE_CACHE_SIZE = 100;

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // Cache 1: kitap id -> kitap (DTO tutuyoruz, entity değil: LAZY sorunu çıkmasın)
    private final Map<Long, BookResponse> bookCache = new ConcurrentHashMap<>();
    // Cache 2: sayfa isteği (page, size, sort) -> o sayfanın sonucu
    private final Map<Pageable, Page<BookResponse>> pageCache = new ConcurrentHashMap<>();

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    public Page<BookResponse> getAllBooks(Pageable pageable) {
        Page<BookResponse> cached = pageCache.get(pageable);
        if (cached != null) {
            log.info("CACHE HIT: kitap sayfası {}", pageable);
            return cached;
        }
        log.info("CACHE MISS: kitap sayfası {}", pageable);
        Page<BookResponse> page = bookRepository.findAll(pageable)
                .map(this::mapToResponse);
        if (pageCache.size() >= MAX_PAGE_CACHE_SIZE) {
            pageCache.clear();          // basit strateji: dolunca hepsini boşalt
        }
        pageCache.put(pageable, page);
        return page;
    }

    public BookResponse getBookById(Long id) {
        BookResponse cached = bookCache.get(id);
        if (cached != null) {
            log.info("CACHE HIT: kitap id={}", id);
            return cached;
        }
        log.info("CACHE MISS: kitap id={}", id);
        BookEntity book = bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir kitap yok: " + id));
        BookResponse response = mapToResponse(book);
        bookCache.put(id, response);
        return response;
    }

    // Bir yazarın kitapları (sayfalı). Bu liste cache'lenmiyor: anahtar yazar id + sayfa bilgisi olurdu.
    public Page<BookResponse> getBooksByAuthor(Long authorId, Pageable pageable) {
        if (authorRepository.findById(authorId).isEmpty()) {
            throw new NoSuchElementException("Böyle bir yazar yok: " + authorId);
        }
        return bookRepository.findByAuthorEntityId(authorId, pageable)
                .map(this::mapToResponse);
    }

    // AuthorService yazar adı değişince çağırır: cache'teki kitaplarda eski authorName kalmasın
    public void clearCache() {
        bookCache.clear();
        pageCache.clear();
    }

    @Transactional
    public BookResponse createBook(BookRequest request, Long authorId) {
        AuthorEntity author = authorRepository.findById(authorId)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir yazar yok: " + authorId));

        BookEntity book = new BookEntity();
        book.setTitle(request.getTitle());
        book.setPageCount(request.getPageCount());
        book.setPrice(request.getPrice());
        book.setAuthorEntity(author);   // kitabı yazarına bağlıyoruz

        BookEntity saved = bookRepository.save(book);
        pageCache.clear();              // sayfalar eskidi: yeni kitap içlerinde yok
        return mapToResponse(saved);
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        BookEntity existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Böyle bir kitap yok: " + id));
        existingBook.setTitle(request.getTitle());
        existingBook.setPageCount(request.getPageCount());
        existingBook.setPrice(request.getPrice());

        BookEntity saved = bookRepository.save(existingBook);
        bookCache.remove(id);
        pageCache.clear();
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new NoSuchElementException("Böyle bir kitap yok: " + id);
        }
        bookRepository.deleteById(id);
        bookCache.remove(id);
        pageCache.clear();
    }

    private BookResponse mapToResponse(BookEntity entity) {
        BookResponse response = new BookResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setPageCount(entity.getPageCount());
        response.setPrice(entity.getPrice());
        response.setAuthorId(entity.getAuthorEntity().getId());
        response.setAuthorName(entity.getAuthorEntity().getName());
        return response;
    }
}