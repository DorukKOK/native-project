package com.example.NativeProject.api.repository;

import com.example.NativeProject.api.model.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Long> {

    Optional<StockEntity> findByBookEntityId(Long bookId);

    @Modifying(clearAutomatically = true) //sorgunun veri değiştirdiğini springe bildirir.
    @Query(value = "UPDATE stocks SET quantity = quantity - :amount " +
            "WHERE book_id = :bookId AND quantity >= :amount",
            nativeQuery = true)
    int decrease(@Param("bookId") Long bookId, @Param("amount") int amount);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE stocks SET quantity = quantity + :amount " +
            "WHERE book_id = :bookId",
            nativeQuery = true)
    int increase(@Param("bookId") Long bookId, @Param("amount") int amount);
}
