package com.example.orderservice.api.model;

public enum OrderStatus {
    PENDING,      // yeni sipariş, henüz onaylanmadı
    CONFIRMED,    // onaylandı
    CANCELLED     // iptal edildi (son durum)
}
