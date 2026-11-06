ALTER TABLE books
    ADD COLUMN stock_quantity INTEGER NOT NULL DEFAULT 0;

-- Mevcut stok değerleri stocks tablosundan books'a taşınır
UPDATE books b
SET stock_quantity = s.quantity
FROM stocks s
WHERE s.book_id = b.id;

ALTER TABLE books
    ADD CONSTRAINT chk_books_stock_quantity CHECK (stock_quantity >= 0);

DROP TABLE stocks;
