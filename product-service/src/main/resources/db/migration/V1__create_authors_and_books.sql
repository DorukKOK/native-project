CREATE TABLE authors (
                         id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         name       VARCHAR(150) NOT NULL,
                         birth_year INTEGER
);

CREATE TABLE books (
                       id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       title      VARCHAR(255) NOT NULL,
                       page_count INTEGER,
                       price      NUMERIC(10, 2),
                       author_id  BIGINT NOT NULL,
                       CONSTRAINT fk_books_author
                           FOREIGN KEY (author_id) REFERENCES authors (id)
                               ON DELETE RESTRICT
);

CREATE INDEX idx_books_author_id ON books (author_id);