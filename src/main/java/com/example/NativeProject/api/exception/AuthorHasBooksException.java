package com.example.NativeProject.api.exception;

public class AuthorHasBooksException extends RuntimeException {
    private final Long authorId;

    public AuthorHasBooksException(Long authorId) {
        super("Bu yazarın kitapları var, silinemez: " + authorId);
        this.authorId = authorId;
    }

    public Long getAuthorId() {
        return authorId;
    }
}