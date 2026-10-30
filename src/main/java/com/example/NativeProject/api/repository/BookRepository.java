package com.example.NativeProject.api.repository;

import com.example.NativeProject.api.model.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
