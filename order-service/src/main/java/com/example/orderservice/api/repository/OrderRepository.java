package com.example.orderservice.api.repository;

import com.example.orderservice.api.model.OrderEntity;
import com.example.orderservice.api.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    // Tek siparişte kalemler aynı sorguda (JOIN) gelir. Sayfalı listede JOIN kullanmıyoruz
    // (koleksiyon + sayfalama bellekte sayfalamaya yol açar), orada @BatchSize devreye girer.
    @Override
    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findById(Long id);

    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
}
