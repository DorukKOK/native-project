-- Test ettiğin kitapların stokları
SELECT id, title, price, stock_quantity
FROM books
WHERE id IN (1, 2, 3, 5, 7, 15, 20)
ORDER BY id;