ALTER TABLE stocks
    ADD CONSTRAINT chk_stocks_quantity CHECK (quantity >= 0);