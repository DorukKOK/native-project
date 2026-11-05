package com.example.NativeProject.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //Kayıt bulunamadı --> 404
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex){
        log.atWarn()
                .addKeyValue("eventCode","RESOURCE_NOT_FOUND")
                .addKeyValue("detail",ex.getMessage())
                .log("Kayıt bulunamadı.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    //Validation hatası --> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidation(MethodArgumentNotValidException ex){
        Map<String,String> errors = new HashMap<>();
        for (FieldError error: ex.getBindingResult().getFieldErrors()){
            errors.put(error.getField(),error.getDefaultMessage());
        }
        log.atWarn()
                .addKeyValue("eventCode","VALIDATION_FAILED")
                .addKeyValue("errors",errors)
                .log("Validasyon Hatası");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    //Yazarın kitapları var, silinemez --> 409
    @ExceptionHandler(AuthorHasBooksException.class)
    public ResponseEntity<String> handleAuthorHasBooks(AuthorHasBooksException ex){
        log.atWarn()
                .addKeyValue("eventCode", "AUTHOR_HAS_BOOKS")
                .addKeyValue("authorId", ex.getAuthorId())
                .log("Yazar silinemedi, kitapları var");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
    // Stok yetersizse (azaltma isteği mevcut stoktan büyükse)--> 409
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<String> HandleInsufficientStockException(InsufficientStockException ex){
        log.atWarn()
                .addKeyValue("eventCode","INSUFFICIENT_STOCK")
                .addKeyValue("bookId",ex.getBookId())
                .log("Yetersiz stok");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    //Veritabanı kısıtlama ihlali --> 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex){
        log.atWarn()
                .addKeyValue("eventCode","DATA_INTEGRITY_VIOLATION")
                .addKeyValue("detail",ex.getMostSpecificCause().getMessage())
                .log("Veri bütünlüğü ihlali");

        return ResponseEntity.status(HttpStatus.CONFLICT).body("veri kısıtlaması nedeniyle işlem yapılamadı");
    }

    //Olmayan adres (örn. /yanlis-url) --> 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResource(NoResourceFoundException ex){
        log.atWarn()
                .addKeyValue("eventCode","ENDPOINT_NOT_FOUND")
                .addKeyValue("path",ex.getResourcePath())
                .log("Endpoint bulunamadı");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Böyle bir adres yok: /" + ex.getResourcePath());
    }

    //Yanlış tipte parametre (örn. /books/abc) --> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex){
        log.atWarn()
                .addKeyValue("eventCode","TYPE_MISMATCH")
                .addKeyValue("parameter",ex.getName())
                .addKeyValue("value",ex.getValue())
                .log("Parametre tipi hatalı");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Geçersiz değer: " + ex.getName() + "=" + ex.getValue());
    }

    //Okunamayan / bozuk JSON gövdesi --> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleNotReadable(HttpMessageNotReadableException ex){
        log.atWarn()
                .addKeyValue("eventCode","MALFORMED_REQUEST")
                .addKeyValue("detail",ex.getMostSpecificCause().getMessage())
                .log("İstek gövdesi okunamadı");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("İstek gövdesi okunamadı, JSON'u kontrol edin");
    }

    //Desteklenmeyen HTTP metodu (örn. POST /books/1) --> 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex){
        log.atWarn()
                .addKeyValue("eventCode","METHOD_NOT_ALLOWED")
                .addKeyValue("method",ex.getMethod())
                .log("HTTP metodu desteklenmiyor");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Bu adres " + ex.getMethod() + " metodunu desteklemiyor");
    }

    //Beklenmedik durumlar --> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Exception ex){
        log.atError()
                .addKeyValue("eventCode","UNEXPECTED_ERROR")
                .setCause(ex)
                .log("Beklenmeyen hata");
        // Detay sadece logda kalır, kullanıcıya iç hata mesajı sızdırmıyoruz
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Beklenmeyen bir hata oluştu");
    }
}