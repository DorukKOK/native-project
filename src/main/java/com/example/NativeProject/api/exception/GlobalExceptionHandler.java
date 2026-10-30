package com.example.NativeProject.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    //Veritabanı kısıtlama ihlali --> 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex){
        log.atWarn()
                .addKeyValue("eventCode","DATA_INTEGRITY_VIOLATION")
                .addKeyValue("detail",ex.getMostSpecificCause().getMessage())
                .log("Veri bütünlüğü ihlali");

        return ResponseEntity.status(HttpStatus.CONFLICT).body("veri kısıtlaması nedeniyle işlem yapılamadı");
    }

    //Beklenmedik durumlar --> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Exception ex){
        log.atError()
                .addKeyValue("eventCode","UNEXPECTED_ERROR")
                .setCause(ex)
                .log("Beklenmeyen hata");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}