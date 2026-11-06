package com.example.NativeProject.api.repository;

import com.example.NativeProject.api.model.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// @EntityGraph: yazarı kitapla aynı sorguda (JOIN) getirir, N+1 sorgu sorunu olmaz
@Repository
public interface BookRepository extends JpaRepository<BookEntity,Long> {
    @Override
    @EntityGraph(attributePaths = "authorEntity")
    Page<BookEntity> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "authorEntity")
    Optional<BookEntity> findById(Long id);

    @EntityGraph(attributePaths = "authorEntity")
    Page<BookEntity> findByAuthorEntityId(Long authorId,Pageable pageable);

    boolean existsByAuthorEntityId(Long authorId);

    @Query("SELECT b.stockQuantity FROM BookEntity b WHERE b.id = :bookId")
    Optional<Integer> findStockQuantityById(@Param("bookId") Long bookId);

    @Modifying(clearAutomatically = true) // sorgunun veri değiştirdiğini springe bildirir.
    @Query(value = "UPDATE books SET stock_quantity = :quantity WHERE id = :bookId", nativeQuery = true)
    int setStock(@Param("bookId") Long bookId, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE books SET stock_quantity = stock_quantity - :amount " +
            "WHERE id = :bookId AND stock_quantity >= :amount", nativeQuery = true)
    int decreaseStock(@Param("bookId") Long bookId, @Param("amount") int amount);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE books SET stock_quantity = stock_quantity + :amount WHERE id = :bookId",
            nativeQuery = true)
    int increaseStock(@Param("bookId") Long bookId, @Param("amount") int amount);

}
