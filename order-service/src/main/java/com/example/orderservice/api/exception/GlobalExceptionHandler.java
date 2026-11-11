package com.example.orderservice.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    //Siparişin durumu bu işleme izin vermiyor (iptal edilmiş siparişi güncelleme vb.) --> 409
    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<String> handleInvalidOrderState(InvalidOrderStateException ex){
        log.atWarn()
                .addKeyValue("eventCode","INVALID_ORDER_STATE")
                .addKeyValue("orderId",ex.getOrderId())
                .log("Siparişin durumu bu işleme uygun değil");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    //Stok yetersiz --> 409
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<String> handleInsufficientStock(InsufficientStockException ex){
        log.atWarn()
                .addKeyValue("eventCode","INSUFFICIENT_STOCK")
                .addKeyValue("bookId",ex.getBookId())
                .log("Stok yetersiz");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
    //Zorunlu header eksik (örn. Idempotency-Key gönderilmedi) --> 400
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException ex){
        log.atWarn()
                .addKeyValue("eventCode","MISSING_HEADER")
                .addKeyValue("header",ex.getHeaderName())
                .log("Zorunlu header eksik");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Zorunlu header eksik: " + ex.getHeaderName());
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

    //Yanlış tipte parametre (örn. /orders/abc, ?status=YANLIS) --> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex){
        log.atWarn()
                .addKeyValue("eventCode","TYPE_MISMATCH")
                .addKeyValue("parameter",ex.getName())
                .addKeyValue("value",ex.getValue())
                .log("Parametre tipi hatalı");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Geçersiz değer: " + ex.getName() + "=" + ex.getValue());
    }

    //Okunamayan / bozuk JSON gövdesi (geçersiz durum adı dahil) --> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleNotReadable(HttpMessageNotReadableException ex){
        log.atWarn()
                .addKeyValue("eventCode","MALFORMED_REQUEST")
                .addKeyValue("detail",ex.getMostSpecificCause().getMessage())
                .log("İstek gövdesi okunamadı");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("İstek gövdesi okunamadı, JSON'u kontrol edin");
    }

    //Desteklenmeyen HTTP metodu (örn. POST /orders/1) --> 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex){
        log.atWarn()
                .addKeyValue("eventCode","METHOD_NOT_ALLOWED")
                .addKeyValue("method",ex.getMethod())
                .log("HTTP metodu desteklenmiyor");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Bu adres " + ex.getMethod() + " metodunu desteklemiyor");
    }

    //Desteklenmeyen içerik tipi (örn. Content-Type: text/plain) --> 415
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex){
        log.atWarn()
                .addKeyValue("eventCode","UNSUPPORTED_MEDIA_TYPE")
                .addKeyValue("contentType",ex.getContentType())
                .log("İçerik tipi desteklenmiyor");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Bu adres yalnızca application/json kabul eder");
    }

    //Olmayan alana göre sıralama (örn. ?sort=yok,asc) --> 400
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<String> handleInvalidSortProperty(PropertyReferenceException ex){
        log.atWarn()
                .addKeyValue("eventCode","INVALID_SORT_PROPERTY")
                .addKeyValue("property",ex.getPropertyName())
                .log("Geçersiz sıralama alanı");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Geçersiz sıralama alanı: " + ex.getPropertyName());
    }

    //product-service'e ulaşılamadı (kapalı / timeout) ya da 5xx döndü --> 503
    @ExceptionHandler({ResourceAccessException.class, HttpServerErrorException.class})
    public ResponseEntity<String> handleProductServiceUnavailable(Exception ex){
        log.atError()
                .addKeyValue("eventCode","PRODUCT_SERVICE_UNAVAILABLE")
                .addKeyValue("detail",ex.getMessage())
                .log("product-service'e ulaşılamadı");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Ürün servisine şu an ulaşılamıyor, lütfen daha sonra tekrar deneyin");
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
