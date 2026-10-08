-- Manual upgrade from the 2026-08-27 production schema to the current app.
-- Run once in psql or pgAdmin against the live database. The application does
-- not apply this file on startup.
--
-- DDL only. Existing rows are not rewritten. New NOT NULL columns carry a
-- default so the older Thymeleaf app can insert without naming them.
-- Safe to run again: objects that already exist are left in place.
--
-- client.king_disc stays. The current app reads king_disc_ymh and king_disc_hnd.

-- ---------------------------------------------------------------------------
-- Category
-- ---------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS category_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS category (
    category_id NUMERIC PRIMARY KEY DEFAULT nextval('category_sequences'::regclass),
    name TEXT NOT NULL,
    parent_id NUMERIC NULL,
    client_id NUMERIC NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (category_id)
);

CREATE INDEX IF NOT EXISTS idx_category_client_deleted ON category (client_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_category_parent ON category (parent_id);

ALTER TABLE product ADD COLUMN IF NOT EXISTS category_id NUMERIC NULL;

DO $$
BEGIN
    ALTER TABLE product
        ALTER COLUMN category_id TYPE NUMERIC
        USING category_id::numeric;
EXCEPTION
    WHEN others THEN NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE product
        ADD CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES category (category_id);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- ---------------------------------------------------------------------------
-- Line product identity. Nullable, no foreign key: old rows keep name text.
-- ---------------------------------------------------------------------------

ALTER TABLE purchasing_detail
    ADD COLUMN IF NOT EXISTS product_id NUMERIC NULL;

ALTER TABLE preorder_detail
    ADD COLUMN IF NOT EXISTS product_id NUMERIC NULL;

ALTER TABLE transaction_detail
    ADD COLUMN IF NOT EXISTS product_id NUMERIC NULL;

CREATE INDEX IF NOT EXISTS idx_purchasing_detail_product_id
    ON purchasing_detail (product_id);

CREATE INDEX IF NOT EXISTS idx_preorder_detail_product_id
    ON preorder_detail (product_id);

CREATE INDEX IF NOT EXISTS idx_transaction_detail_product_id
    ON transaction_detail (product_id);

CREATE INDEX IF NOT EXISTS idx_product_client_names
    ON product (client_id, short_name, full_name)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Product sets and pretel lines
-- ---------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS product_set_sequences START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS product_set_component_sequences START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS transaction_pretel_sequences START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS transaction_pretel_component_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS product_set (
    product_set_id NUMERIC PRIMARY KEY DEFAULT nextval('product_set_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    parent_product_id NUMERIC NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_product_set_parent_active
    ON product_set (client_id, parent_product_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_product_set_client_deleted
    ON product_set (client_id, deleted_at);

CREATE TABLE IF NOT EXISTS product_set_component (
    product_set_component_id NUMERIC PRIMARY KEY DEFAULT nextval('product_set_component_sequences'::regclass),
    product_set_id NUMERIC NOT NULL,
    child_product_id NUMERIC NOT NULL,
    qty NUMERIC NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_product_set_component_child_active
    ON product_set_component (product_set_id, child_product_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_product_set_component_set
    ON product_set_component (product_set_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS transaction_pretel (
    transaction_pretel_id NUMERIC PRIMARY KEY DEFAULT nextval('transaction_pretel_sequences'::regclass),
    transaction_id NUMERIC NOT NULL,
    client_id NUMERIC NOT NULL,
    parent_product_id NUMERIC NOT NULL,
    set_qty NUMERIC NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_transaction_pretel_transaction
    ON transaction_pretel (transaction_id);

CREATE TABLE IF NOT EXISTS transaction_pretel_component (
    transaction_pretel_component_id NUMERIC PRIMARY KEY DEFAULT nextval('transaction_pretel_component_sequences'::regclass),
    transaction_pretel_id NUMERIC NOT NULL,
    child_product_id NUMERIC NOT NULL,
    qty_in NUMERIC NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_transaction_pretel_component_header
    ON transaction_pretel_component (transaction_pretel_id);

-- ---------------------------------------------------------------------------
-- Payment methods. Table only; no seed rows.
-- ---------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS payment_method_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS payment_method (
    payment_method_id NUMERIC PRIMARY KEY DEFAULT nextval('payment_method_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    name TEXT NOT NULL,
    method_type TEXT NOT NULL,
    rekening TEXT NOT NULL DEFAULT '',
    is_system_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT payment_method_type_check CHECK (
        method_type IN ('cash', 'transfer', 'credit', 'qris')
    )
);

CREATE INDEX IF NOT EXISTS idx_payment_method_client_deleted
    ON payment_method (client_id, deleted_at);

-- ---------------------------------------------------------------------------
-- Sale and inden payment columns.
-- Defaults let an older insert omit the new columns.
-- ---------------------------------------------------------------------------

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS is_cash BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS is_paid BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS paid_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS due_date TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS payment_method_id NUMERIC;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS payment_method_name TEXT;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS payment_method_type TEXT;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS payment_method_rekening TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS is_cash BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS is_paid BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS paid_amount NUMERIC NOT NULL DEFAULT 0;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS due_date TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS payment_method_id NUMERIC;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS payment_method_name TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS payment_method_type TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS payment_method_rekening TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS bukti_original_name TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS bukti_file_path TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS sale_posted BOOLEAN NOT NULL DEFAULT FALSE;

-- ---------------------------------------------------------------------------
-- Payment proof may belong to a purchase or a sale, never both.
-- Existing purchase proofs have pembelian_id set and transaction_id empty.
-- ---------------------------------------------------------------------------

ALTER TABLE bukti_bayar
    ALTER COLUMN pembelian_id DROP NOT NULL;

ALTER TABLE bukti_bayar
    ADD COLUMN IF NOT EXISTS transaction_id NUMERIC;

ALTER TABLE bukti_bayar
    DROP CONSTRAINT IF EXISTS bukti_bayar_one_parent;

ALTER TABLE bukti_bayar
    ADD CONSTRAINT bukti_bayar_one_parent CHECK (
        (pembelian_id IS NOT NULL AND transaction_id IS NULL)
        OR (pembelian_id IS NULL AND transaction_id IS NOT NULL)
    );

-- ---------------------------------------------------------------------------
-- Split payments on a sale
-- ---------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS transaction_payment_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS transaction_payment (
    transaction_payment_id NUMERIC PRIMARY KEY,
    transaction_id NUMERIC NOT NULL,
    payment_method_id NUMERIC,
    payment_method_name TEXT,
    payment_method_type TEXT,
    payment_method_rekening TEXT,
    amount NUMERIC NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transaction_payment_transaction_id
    ON transaction_payment (transaction_id);

DROP TRIGGER IF EXISTS trigger_created_at ON transaction_payment;
CREATE TRIGGER trigger_created_at
    BEFORE INSERT ON transaction_payment
    FOR EACH ROW
    EXECUTE FUNCTION created_at();

DROP TRIGGER IF EXISTS trigger_updated_at ON transaction_payment;
CREATE TRIGGER trigger_updated_at
    BEFORE UPDATE ON transaction_payment
    FOR EACH ROW
    EXECUTE FUNCTION updated_at();

-- ---------------------------------------------------------------------------
-- Client discounts used by the current app. king_disc is left untouched.
-- ---------------------------------------------------------------------------

ALTER TABLE client
    ADD COLUMN IF NOT EXISTS king_disc_ymh NUMERIC;

ALTER TABLE client
    ADD COLUMN IF NOT EXISTS king_disc_hnd NUMERIC;
