ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE orders ADD CONSTRAINT uc_orders_idempotency_key UNIQUE (idempotency_key);