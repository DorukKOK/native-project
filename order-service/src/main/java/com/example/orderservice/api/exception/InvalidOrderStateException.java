package com.example.orderservice.api.exception;

// Siparişin mevcut durumu bu işleme izin vermiyorsa fırlatılır (örn. iptal edilmiş siparişi güncellemek)
public class InvalidOrderStateException extends RuntimeException {
    private final Long orderId;

    public InvalidOrderStateException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

    public Long getOrderId() {return orderId;}
}
