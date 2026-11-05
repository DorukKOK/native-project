package com.example.NativeProject.api.repository;

import com.example.NativeProject.api.model.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Long> {

    Optional<StockEntity> findByBookEntityId(Long bookId);

    boolean existsByBookEntityId(Long bookId);

    // Verilen kitapların stoğunu tek sorguda getirir: her satır [bookId, quantity]. Entity yüklemez, N+1 olmaz.
    @Query("SELECT s.bookEntity.id, s.quantity FROM StockEntity s WHERE s.bookEntity.id IN :bookIds")
    List<Object[]> findQuantitiesByBookIds(@Param("bookIds") Collection<Long> bookIds);

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

