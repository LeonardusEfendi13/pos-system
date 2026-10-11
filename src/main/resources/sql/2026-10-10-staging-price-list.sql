-- Staging for Honda/Yamaha price-list Excel imports.
-- Confirm copies READY rows into price_list in one transaction, then clears the merk.

CREATE TABLE IF NOT EXISTS staging_price_list (
    staging_price_list_id BIGSERIAL PRIMARY KEY,
    merk VARCHAR(16) NOT NULL,
    part_number VARCHAR(255),
    nama TEXT,
    harga NUMERIC(18, 2),
    harga_text VARCHAR(255),
    excel_row INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_staging_price_list_merk
    ON staging_price_list (merk);
