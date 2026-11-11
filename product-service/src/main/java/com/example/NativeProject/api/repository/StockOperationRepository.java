package com.example.NativeProject.api.repository;

import com.example.NativeProject.api.model.StockOperationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockOperationRepository extends JpaRepository<StockOperationsEntity,Long> {
    Optional<StockOperationsEntity> findByIdempotencyKey(String idempotencyKey);
}
