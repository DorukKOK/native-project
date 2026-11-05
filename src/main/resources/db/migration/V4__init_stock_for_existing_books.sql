INSERT INTO stocks (book_id, quantity)
SELECT b.id, 0
FROM books b
WHERE NOT EXISTS (SELECT 1 FROM stocks s WHERE s.book_id = b.id);