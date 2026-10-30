package com.example.NativeProject.api.repository;

import com.example.NativeProject.api.model.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<BookEntity,Long> {
    Page<BookEntity> findByAuthorEntityId(Long authorId,Pageable pageable);
    boolean existsByAuthorEntityId(Long authorId);

}
