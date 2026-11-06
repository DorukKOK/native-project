CREATE TABLE orders (
                        id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        customer_name VARCHAR(150)             NOT NULL,
                        status        VARCHAR(20)              NOT NULL,
                        total_amount  NUMERIC(12, 2)           NOT NULL,
                        created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                        CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
                        CONSTRAINT chk_orders_total_amount CHECK (total_amount >= 0)
);

CREATE INDEX idx_orders_status ON orders (status);

-- book_id product-service'teki kitabın id'sidir. Başka servisin veritabanında olduğu için
-- FOREIGN KEY yok: servisler birbirinin tablosuna bağlanmaz, yalnızca id ile haberleşir.
CREATE TABLE order_items (
                             id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             order_id   BIGINT         NOT NULL,
                             book_id    BIGINT         NOT NULL,
                             quantity   INTEGER        NOT NULL,
                             unit_price NUMERIC(10, 2) NOT NULL,
                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id) REFERENCES orders (id)
                                     ON DELETE CASCADE,
                             CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
                             CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
