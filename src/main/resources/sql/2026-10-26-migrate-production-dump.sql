-- One-file migration: production dump (dump-postgres-202610260844) -> current schema.
--
-- HOW TO RUN (on a fresh COPY, never on the live production database):
--   1. createdb pos_anugrah_new
--   2. pg_restore -U postgres -d pos_anugrah_new --no-owner dump-postgres-202610260844.sql
--   3. psql -U postgres -d pos_anugrah_new -f 2026-10-26-migrate-production-dump.sql
--   4. Read the verification output at the end, then point the backend at pos_anugrah_new.
--
-- Needs a superuser: it switches row triggers off so updated_at / created_at of old rows
-- stay untouched. Everything up to the COMMIT is one transaction: any error rolls the whole
-- migration back and leaves the copy exactly as restored. Safe to run again.
-- Expect a few minutes: the old tables have no indexes, so step 1 builds them first.

\set ON_ERROR_STOP on
\timing on

BEGIN;
SET LOCAL session_replication_role = replica;

-- ===========================================================================
-- STEP 1. Indexes the dump lacks (the line-product backfill joins on these)
-- ===========================================================================

CREATE INDEX IF NOT EXISTS mig_idx_transaction_id ON "transaction" (transaction_id);
CREATE INDEX IF NOT EXISTS mig_idx_transaction_client ON "transaction" (client_id);
CREATE INDEX IF NOT EXISTS mig_idx_transaction_detail_txn ON transaction_detail (transaction_id);
CREATE INDEX IF NOT EXISTS mig_idx_purchasing_id ON purchasing (purchasing_id);
CREATE INDEX IF NOT EXISTS mig_idx_purchasing_detail_pur ON purchasing_detail (purchasing_id);
CREATE INDEX IF NOT EXISTS mig_idx_preorder_id ON preorder (preorder_id);
CREATE INDEX IF NOT EXISTS mig_idx_preorder_detail_po ON preorder_detail (preorder_id);
CREATE INDEX IF NOT EXISTS mig_idx_product_id ON product (product_id);
ANALYZE;

-- ===========================================================================
-- STEP 2. Schema upgrade and role/menu seed (the existing scripts, in order)
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- from 2026-09-13-category.sql
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
-- from 2026-09-20-detail-product-id.sql
-- ---------------------------------------------------------------------------

-- Add nullable product_id on line tables so current Hibernate mappings can insert
-- without breaking historical rows from the client dump (identity was short_name/full_name).
-- No NOT NULL and no FK: old rows may not match a live product; old inserts that omit
-- the column still succeed.

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

-- Backfill only when exactly one live product matches the stored names for that client.
WITH purchasing_match AS (
    SELECT
        pd.purchasing_detail_id,
        MIN(p.product_id) AS product_id
    FROM purchasing_detail pd
    JOIN purchasing pur ON pur.purchasing_id = pd.purchasing_id
    JOIN product p
        ON p.client_id = pur.client_id
        AND p.short_name = pd.short_name
        AND p.full_name = pd.full_name
        AND p.deleted_at IS NULL
    WHERE pd.product_id IS NULL
    GROUP BY pd.purchasing_detail_id
    HAVING COUNT(DISTINCT p.product_id) = 1
)
UPDATE purchasing_detail pd
SET product_id = purchasing_match.product_id
FROM purchasing_match
WHERE pd.purchasing_detail_id = purchasing_match.purchasing_detail_id;

WITH preorder_match AS (
    SELECT
        pd.preorder_detail_id,
        MIN(p.product_id) AS product_id
    FROM preorder_detail pd
    JOIN preorder po ON po.preorder_id = pd.preorder_id
    JOIN product p
        ON p.client_id = po.client_id
        AND p.short_name = pd.short_name
        AND p.full_name = pd.full_name
        AND p.deleted_at IS NULL
    WHERE pd.product_id IS NULL
    GROUP BY pd.preorder_detail_id
    HAVING COUNT(DISTINCT p.product_id) = 1
)
UPDATE preorder_detail pd
SET product_id = preorder_match.product_id
FROM preorder_match
WHERE pd.preorder_detail_id = preorder_match.preorder_detail_id;

-- Penjualan: ~100k rows, run this last in pgAdmin if needed. Safe to stop and resume;
-- already-filled rows are skipped. Old app keeps working with NULL product_id.
UPDATE transaction_detail td
SET product_id = p.product_id
FROM "transaction" t
JOIN product p
    ON p.client_id = t.client_id
    AND p.deleted_at IS NULL
WHERE t.transaction_id = td.transaction_id
    AND p.short_name = td.short_name
    AND p.full_name = td.full_name
    AND td.product_id IS NULL;


-- ---------------------------------------------------------------------------
-- from 2026-09-28-product-category-barang.sql
-- ---------------------------------------------------------------------------

-- One-time: every product points at its client's root category named BARANG.
-- Creates that root category when the client does not already have one.
-- Run once against the database. This script is not applied on application startup.

INSERT INTO category (name, parent_id, client_id, created_at, updated_at)
SELECT 'BARANG', NULL, c.client_id, NOW(), NOW()
FROM client c
WHERE NOT EXISTS (
    SELECT 1
    FROM category existing
    WHERE existing.client_id = c.client_id
      AND existing.deleted_at IS NULL
      AND existing.parent_id IS NULL
      AND LOWER(TRIM(existing.name)) = 'barang'
);

UPDATE product AS p
SET category_id = chosen.category_id,
    updated_at = NOW()
FROM (
    SELECT DISTINCT ON (client_id) category_id, client_id
    FROM category
    WHERE deleted_at IS NULL
      AND parent_id IS NULL
      AND LOWER(TRIM(name)) = 'barang'
    ORDER BY client_id, category_id
) AS chosen
WHERE chosen.client_id = p.client_id;


-- ---------------------------------------------------------------------------
-- from 2026-09-29-product-set.sql
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
-- from 2026-09-30-payment-method.sql
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

INSERT INTO payment_method (
    client_id,
    name,
    method_type,
    rekening,
    is_system_default,
    created_at,
    updated_at
)
SELECT c.client_id, v.name, v.method_type, '', v.is_default, NOW(), NOW()
FROM client c
CROSS JOIN (
    VALUES
        ('Cash', 'cash', TRUE),
        ('Transfer', 'transfer', FALSE),
        ('Credit', 'credit', FALSE),
        ('Qris', 'qris', FALSE)
) AS v(name, method_type, is_default)
WHERE NOT EXISTS (
    SELECT 1
    FROM payment_method pm
    WHERE pm.client_id = c.client_id
      AND pm.deleted_at IS NULL
      AND pm.name = v.name
      AND pm.method_type = v.method_type
);

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
    ADD COLUMN IF NOT EXISTS paid_amount NUMERIC;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS due_date TIMESTAMP WITHOUT TIME ZONE;

UPDATE inden
SET is_cash = COALESCE(deposit, 0) >= COALESCE(total_price, 0),
    is_paid = COALESCE(deposit, 0) >= COALESCE(total_price, 0),
    paid_amount = LEAST(COALESCE(deposit, 0), COALESCE(total_price, 0)),
    due_date = NULL
WHERE paid_amount IS NULL;

ALTER TABLE inden
    ALTER COLUMN paid_amount SET DEFAULT 0;

UPDATE inden
SET paid_amount = 0
WHERE paid_amount IS NULL;

ALTER TABLE inden
    ALTER COLUMN paid_amount SET NOT NULL;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS payment_method_id NUMERIC;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS payment_method_name TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS payment_method_type TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS payment_method_rekening TEXT;

UPDATE inden
SET is_cash = COALESCE(deposit, 0) >= COALESCE(total_price, 0),
    is_paid = COALESCE(deposit, 0) >= COALESCE(total_price, 0),
    paid_amount = LEAST(COALESCE(deposit, 0), COALESCE(total_price, 0)),
    due_date = NULL
WHERE payment_method_id IS NULL;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS bukti_original_name TEXT;

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS bukti_file_path TEXT;


-- ---------------------------------------------------------------------------
-- from 2026-09-30-sale-credit.sql
-- ---------------------------------------------------------------------------

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS is_cash BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS is_paid BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS paid_amount NUMERIC;

ALTER TABLE transaction
    ADD COLUMN IF NOT EXISTS due_date TIMESTAMP WITHOUT TIME ZONE;

UPDATE transaction
SET is_cash = TRUE,
    is_paid = TRUE,
    paid_amount = COALESCE(total_price, 0),
    due_date = NULL
WHERE paid_amount IS NULL;

ALTER TABLE transaction
    ALTER COLUMN paid_amount SET DEFAULT 0;

UPDATE transaction
SET paid_amount = 0
WHERE paid_amount IS NULL;

ALTER TABLE transaction
    ALTER COLUMN paid_amount SET NOT NULL;

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
-- from 2026-10-07-inden-sale-posted.sql
-- ---------------------------------------------------------------------------

ALTER TABLE inden
    ADD COLUMN IF NOT EXISTS sale_posted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE inden
SET sale_posted = TRUE
WHERE UPPER(status_inden) = 'DISERAHKAN';


-- ---------------------------------------------------------------------------
-- from 2026-10-08-from-aug27.sql
-- ---------------------------------------------------------------------------

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


-- ---------------------------------------------------------------------------
-- from 2026-10-09-role-menu.sql
-- ---------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS client_role_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS client_role (
    client_role_id NUMERIC PRIMARY KEY DEFAULT nextval('client_role_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    name TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    seeded BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_client_role_name UNIQUE (client_id, name)
);

CREATE TABLE IF NOT EXISTS role_menu_grant (
    client_role_id NUMERIC NOT NULL,
    menu_key TEXT NOT NULL,
    CONSTRAINT pk_role_menu_grant PRIMARY KEY (client_role_id, menu_key)
);

ALTER TABLE account ADD COLUMN IF NOT EXISTS client_role_id NUMERIC NULL;

ALTER TABLE client_role ADD COLUMN IF NOT EXISTS seeded BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO client_role (client_id, name, created_at, updated_at)
SELECT DISTINCT a.client_id, seed.name, NOW(), NOW()
FROM account a
CROSS JOIN (VALUES ('ADMIN_1'), ('ADMIN_2'), ('ADMIN_3')) AS seed(name)
WHERE NOT EXISTS (
    SELECT 1
    FROM client_role existing
    WHERE existing.client_id = a.client_id
      AND existing.name = seed.name
);

INSERT INTO role_menu_grant (client_role_id, menu_key)
SELECT client_role_row.client_role_id, seed.menu_key
FROM client_role client_role_row
CROSS JOIN (VALUES ('kasir'), ('penjualan')) AS seed(menu_key)
WHERE client_role_row.name IN ('ADMIN_1', 'ADMIN_2', 'ADMIN_3')
  AND client_role_row.seeded = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_grant grant_row
      WHERE grant_row.client_role_id = client_role_row.client_role_id
  );

UPDATE client_role
SET seeded = TRUE
WHERE name IN ('ADMIN_1', 'ADMIN_2', 'ADMIN_3')
  AND seeded = FALSE;

UPDATE account AS target
SET client_role_id = client_role_row.client_role_id
FROM client_role AS client_role_row
WHERE target.client_role_id IS NULL
  AND target.role::text IN ('ADMIN_1', 'ADMIN_2', 'ADMIN_3')
  AND client_role_row.client_id = target.client_id
  AND client_role_row.name = target.role::text;

UPDATE account
SET role = 'SUPER_ADMIN'
WHERE role::text = 'GOD_ADMIN';


-- ---------------------------------------------------------------------------
-- from 2026-10-10-staging-price-list.sql
-- ---------------------------------------------------------------------------

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


-- ---------------------------------------------------------------------------
-- from 2026-10-13-staff-hr.sql
-- ---------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS staff_sequences START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS staff_attendance_sequences START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS payroll_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS staff (
    staff_id NUMERIC PRIMARY KEY DEFAULT nextval('staff_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    nama TEXT NOT NULL,
    nik TEXT NOT NULL DEFAULT '',
    tempat_lahir TEXT NOT NULL DEFAULT '',
    tanggal_lahir DATE,
    tanggal_join DATE,
    tanggal_resign DATE,
    jabatan TEXT NOT NULL,
    gaji_pokok NUMERIC NOT NULL DEFAULT 0,
    uang_makan NUMERIC NOT NULL DEFAULT 0,
    insentif NUMERIC NOT NULL DEFAULT 0,
    no_hp TEXT NOT NULL DEFAULT '',
    no_hp_darurat TEXT NOT NULL DEFAULT '',
    jenis_kelamin TEXT NOT NULL,
    pendidikan_terakhir TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT staff_jabatan_check CHECK (
        jabatan IN (
            'KASIR',
            'PROBATION',
            'KARYAWAN_ENTRY',
            'KARYAWAN_JUNIOR',
            'KARYAWAN_SENIOR'
        )
    ),
    CONSTRAINT staff_jenis_kelamin_check CHECK (
        jenis_kelamin IN ('PRIA', 'WANITA')
    ),
    CONSTRAINT staff_pendidikan_check CHECK (
        pendidikan_terakhir IN ('SD', 'SMP', 'SMA', 'S1', 'TIDAK_SEKOLAH')
    )
);

CREATE INDEX IF NOT EXISTS idx_staff_client_deleted
    ON staff (client_id, deleted_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_client_nik
    ON staff (client_id, lower(nik))
    WHERE deleted_at IS NULL AND nik <> '';

CREATE TABLE IF NOT EXISTS staff_attendance (
    attendance_id NUMERIC PRIMARY KEY DEFAULT nextval('staff_attendance_sequences'::regclass),
    staff_id NUMERIC NOT NULL,
    status TEXT NOT NULL,
    keterangan TEXT,
    attendance_date DATE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT staff_attendance_status_check CHECK (
        status IN ('HADIR', 'IZIN', 'SAKIT', 'ALFA')
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_attendance_day
    ON staff_attendance (staff_id, attendance_date);

CREATE INDEX IF NOT EXISTS idx_staff_attendance_date
    ON staff_attendance (attendance_date);

CREATE TABLE IF NOT EXISTS payroll (
    payroll_id NUMERIC PRIMARY KEY DEFAULT nextval('payroll_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    staff_id NUMERIC NOT NULL,
    staff_name TEXT NOT NULL,
    payroll_month INTEGER NOT NULL,
    payroll_year INTEGER NOT NULL,
    days_in_month INTEGER NOT NULL,
    absence_count INTEGER NOT NULL,
    deducted_days INTEGER NOT NULL,
    daily_rate NUMERIC NOT NULL,
    gaji_pokok NUMERIC NOT NULL,
    uang_makan NUMERIC NOT NULL,
    insentif NUMERIC NOT NULL,
    potongan NUMERIC NOT NULL,
    gaji_pokok_dibayar NUMERIC NOT NULL,
    total_payroll NUMERIC NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_payroll_client_month
    ON payroll (client_id, payroll_year, payroll_month, deleted_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payroll_staff_month
    ON payroll (staff_id, payroll_year, payroll_month)
    WHERE deleted_at IS NULL;


-- ---------------------------------------------------------------------------
-- from 2026-10-18-staff-jabatan-payroll-paid.sql
-- ---------------------------------------------------------------------------

-- Jabatan is now Probation / Junior / Senior; Kasir and Entry staff move to Probation
UPDATE staff
SET jabatan = 'PROBATION'
WHERE jabatan IN ('KASIR', 'KARYAWAN_ENTRY');

ALTER TABLE staff DROP CONSTRAINT IF EXISTS staff_jabatan_check;
ALTER TABLE staff ADD CONSTRAINT staff_jabatan_check CHECK (
    jabatan IN ('PROBATION', 'KARYAWAN_JUNIOR', 'KARYAWAN_SENIOR')
);

-- Payroll is calculated live; a row is only written when a month is marked paid
ALTER TABLE payroll ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITHOUT TIME ZONE;


-- ---------------------------------------------------------------------------
-- from 2026-10-20-payment-types.sql
-- ---------------------------------------------------------------------------

-- Payment types: a free name per client plus a fixed behavior that drives checkout
CREATE SEQUENCE IF NOT EXISTS payment_type_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS payment_type (
    payment_type_id NUMERIC PRIMARY KEY DEFAULT nextval('payment_type_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    name TEXT NOT NULL,
    behavior TEXT NOT NULL,
    is_system_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT payment_type_behavior_check CHECK (
        behavior IN ('cash', 'transfer', 'credit', 'qris')
    )
);

CREATE INDEX IF NOT EXISTS idx_payment_type_client_deleted
    ON payment_type (client_id, deleted_at);

DROP TRIGGER IF EXISTS trigger_created_at ON payment_type;
CREATE TRIGGER trigger_created_at
    BEFORE INSERT ON payment_type
    FOR EACH ROW
    EXECUTE FUNCTION created_at();

DROP TRIGGER IF EXISTS trigger_updated_at ON payment_type;
CREATE TRIGGER trigger_updated_at
    BEFORE UPDATE ON payment_type
    FOR EACH ROW
    EXECUTE FUNCTION updated_at();

-- Seed the four starting types for clients that have none yet
INSERT INTO payment_type (client_id, name, behavior, is_system_default)
SELECT c.client_id, seed.name, seed.behavior, seed.is_system_default
FROM client c
CROSS JOIN (
    VALUES
        (1, 'Cash', 'cash', TRUE),
        (2, 'Transfer', 'transfer', FALSE),
        (3, 'QRIS', 'qris', FALSE),
        (4, 'Kredit', 'credit', FALSE)
) AS seed(sort_order, name, behavior, is_system_default)
WHERE NOT EXISTS (
    SELECT 1 FROM payment_type pt WHERE pt.client_id = c.client_id
)
ORDER BY c.client_id, seed.sort_order;

-- Link payment methods to the type matching their behavior
ALTER TABLE payment_method ADD COLUMN IF NOT EXISTS payment_type_id NUMERIC;

UPDATE payment_method pm
SET payment_type_id = pt.payment_type_id
FROM payment_type pt
WHERE pm.payment_type_id IS NULL
  AND pt.client_id = pm.client_id
  AND pt.behavior = pm.method_type
  AND pt.deleted_at IS NULL;

ALTER TABLE payment_method ALTER COLUMN payment_type_id SET NOT NULL;

-- Type name snapshots so reports keep the name used at sale time
ALTER TABLE transaction_payment ADD COLUMN IF NOT EXISTS payment_type_name TEXT;
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS payment_type_name TEXT;
ALTER TABLE inden ADD COLUMN IF NOT EXISTS payment_type_name TEXT;

UPDATE transaction_payment
SET payment_type_name = CASE LOWER(COALESCE(payment_method_type, ''))
    WHEN 'transfer' THEN 'Transfer'
    WHEN 'qris' THEN 'QRIS'
    WHEN 'credit' THEN 'Kredit'
    ELSE 'Cash'
END
WHERE payment_type_name IS NULL;

-- Credit sales stay under Kredit even after they are settled
UPDATE transaction
SET payment_type_name = CASE
    WHEN is_cash = FALSE THEN 'Kredit'
    ELSE CASE LOWER(COALESCE(payment_method_type, ''))
        WHEN 'transfer' THEN 'Transfer'
        WHEN 'qris' THEN 'QRIS'
        WHEN 'credit' THEN 'Kredit'
        ELSE 'Cash'
    END
END
WHERE payment_type_name IS NULL;

UPDATE inden
SET payment_type_name = CASE LOWER(payment_method_type)
    WHEN 'cash' THEN 'Cash'
    WHEN 'transfer' THEN 'Transfer'
    WHEN 'qris' THEN 'QRIS'
    WHEN 'credit' THEN 'Kredit'
END
WHERE payment_type_name IS NULL
  AND COALESCE(payment_method_type, '') <> '';


-- ---------------------------------------------------------------------------
-- from 2026-10-21-staff-insentif-payout.sql
-- ---------------------------------------------------------------------------

-- Insentif is no longer part of the monthly salary: each paid month saves it in
-- full, and a payout cashes out every saved month of one staff at once
CREATE SEQUENCE IF NOT EXISTS insentif_payout_sequences START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS insentif_payout (
    insentif_payout_id NUMERIC PRIMARY KEY DEFAULT nextval('insentif_payout_sequences'::regclass),
    client_id NUMERIC NOT NULL,
    staff_id NUMERIC NOT NULL,
    staff_name TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    month_count INTEGER NOT NULL,
    paid_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE payroll ADD COLUMN IF NOT EXISTS insentif_payout_id NUMERIC;

-- Roles that can open Payroll also get the new Insentif page
INSERT INTO role_menu_grant (client_role_id, menu_key)
SELECT grant_row.client_role_id, 'staff.insentif'
FROM role_menu_grant grant_row
WHERE grant_row.menu_key = 'staff.payroll'
ON CONFLICT DO NOTHING;


-- ===========================================================================
-- STEP 3. New data for old rows
-- ===========================================================================

-- New rows created while triggers are off have no timestamps yet
UPDATE payment_type SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL;

-- Every old sale was paid in cash: one payment line per sale, same amount, dated like the sale
INSERT INTO transaction_payment (
    transaction_payment_id, transaction_id, payment_method_id, payment_method_name,
    payment_method_type, payment_method_rekening, payment_type_name, amount, sort_order,
    created_at, updated_at
)
SELECT nextval('transaction_payment_sequences'), t.transaction_id, pm.payment_method_id, pm.name,
       pm.method_type, pm.rekening, pt.name, COALESCE(t.total_price, 0), 1,
       t.created_at, t.created_at
FROM "transaction" t
JOIN payment_method pm
    ON pm.client_id = t.client_id
   AND pm.method_type = 'cash'
   AND pm.is_system_default
   AND pm.deleted_at IS NULL
JOIN payment_type pt ON pt.payment_type_id = pm.payment_type_id
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_payment tp WHERE tp.transaction_id = t.transaction_id
);

-- The sale header shows the same method
UPDATE "transaction" t
SET payment_method_id = pm.payment_method_id,
    payment_method_name = pm.name,
    payment_method_type = pm.method_type,
    payment_method_rekening = pm.rekening
FROM payment_method pm
WHERE t.payment_method_id IS NULL
  AND pm.client_id = t.client_id
  AND pm.method_type = 'cash'
  AND pm.is_system_default
  AND pm.deleted_at IS NULL;

-- Move every sequence past the highest existing id so the first new insert cannot collide
DO $$
DECLARE
    item RECORD;
    max_id NUMERIC;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('account_sequences', 'account', 'account_id'),
            ('branch_sequences', 'branch', 'id'),
            ('bukti_bayar_sequences', 'bukti_bayar', 'bukti_bayar_id'),
            ('category_sequences', 'category', 'category_id'),
            ('client_role_sequences', 'client_role', 'client_role_id'),
            ('compatible_products_sequences', 'compatible_products', 'id'),
            ('customer_sequences', 'customer', 'customer_id'),
            ('inden_sequences', 'inden', 'id'),
            ('inden_detail_sequences', 'inden_detail', 'inden_detail_id'),
            ('login_token_sequences', 'login_token', 'token_id'),
            ('payment_method_sequences', 'payment_method', 'payment_method_id'),
            ('payment_type_sequences', 'payment_type', 'payment_type_id'),
            ('preorder_sequences', 'preorder', 'preorder_id'),
            ('preorder_detail_sequences', 'preorder_detail', 'preorder_detail_id'),
            ('product_sequences', 'product', 'product_id'),
            ('product_prices_sequences', 'product_prices', 'product_prices_id'),
            ('purchasing_sequences', 'purchasing', 'purchasing_id'),
            ('purchasing_detail_sequences', 'purchasing_detail', 'purchasing_detail_id'),
            ('stock_movements_sequences', 'stock_movements', 'stock_movements_id'),
            ('supplier_sequences', 'supplier', 'supplier_id'),
            ('transaction_sequences', 'transaction', 'transaction_id'),
            ('transaction_detail_sequences', 'transaction_detail', 'transaction_detail_id'),
            ('transaction_payment_sequences', 'transaction_payment', 'transaction_payment_id'),
            ('vehicle_sequences', 'vehicle', 'id')
        ) AS v(seq_name, table_name, id_column)
    LOOP
        EXECUTE format('SELECT MAX(%I) FROM %I', item.id_column, item.table_name) INTO max_id;
        IF max_id IS NOT NULL THEN
            PERFORM setval(
                item.seq_name::regclass,
                GREATEST(max_id, COALESCE((SELECT last_value FROM pg_sequences WHERE sequencename = item.seq_name), 1))::bigint
            );
        END IF;
    END LOOP;
END $$;

COMMIT;

-- ===========================================================================
-- STEP 4. Verification (read-only). Run the same counts on the original database and compare
-- ===========================================================================

\echo '--- 4.1 Row counts (must equal the old database)'
SELECT 'account' AS tbl, COUNT(*) FROM account UNION ALL
SELECT 'client', COUNT(*) FROM client UNION ALL
SELECT 'customer', COUNT(*) FROM customer UNION ALL
SELECT 'supplier', COUNT(*) FROM supplier UNION ALL
SELECT 'product', COUNT(*) FROM product UNION ALL
SELECT 'product_prices', COUNT(*) FROM product_prices UNION ALL
SELECT 'stock_movements', COUNT(*) FROM stock_movements UNION ALL
SELECT 'transaction', COUNT(*) FROM "transaction" UNION ALL
SELECT 'transaction_detail', COUNT(*) FROM transaction_detail UNION ALL
SELECT 'purchasing', COUNT(*) FROM purchasing UNION ALL
SELECT 'purchasing_detail', COUNT(*) FROM purchasing_detail UNION ALL
SELECT 'preorder', COUNT(*) FROM preorder UNION ALL
SELECT 'preorder_detail', COUNT(*) FROM preorder_detail UNION ALL
SELECT 'inden', COUNT(*) FROM inden UNION ALL
SELECT 'inden_detail', COUNT(*) FROM inden_detail UNION ALL
SELECT 'bukti_bayar', COUNT(*) FROM bukti_bayar UNION ALL
SELECT 'vehicle', COUNT(*) FROM vehicle UNION ALL
SELECT 'branch', COUNT(*) FROM branch UNION ALL
SELECT 'compatible_products', COUNT(*) FROM compatible_products
ORDER BY 1;

\echo '--- 4.2 Money totals (must equal the old database)'
SELECT SUM(total_price) AS sales_total, SUM(paid_amount) AS sales_paid FROM "transaction";
SELECT SUM(total_price) AS purchasing_total FROM purchasing;
SELECT SUM(total_price) AS inden_total FROM inden;

\echo '--- 4.3 Users, roles and menu access'
SELECT a.account_id, a.user_name, a.role, cr.name AS client_role,
       COALESCE(STRING_AGG(g.menu_key, ', ' ORDER BY g.menu_key), '(all menus)') AS menus
FROM account a
LEFT JOIN client_role cr ON cr.client_role_id = a.client_role_id
LEFT JOIN role_menu_grant g ON g.client_role_id = a.client_role_id
GROUP BY a.account_id, a.user_name, a.role, cr.name
ORDER BY a.account_id;

\echo '--- 4.4 Payments: sales without a payment line, and total difference (both must be 0)'
SELECT COUNT(*) AS sales_without_payment
FROM "transaction" t
WHERE NOT EXISTS (SELECT 1 FROM transaction_payment tp WHERE tp.transaction_id = t.transaction_id);
SELECT (SELECT COALESCE(SUM(total_price), 0) FROM "transaction")
     - (SELECT COALESCE(SUM(amount), 0) FROM transaction_payment) AS payment_difference;

\echo '--- 4.5 Sale lines not linked to a product (left empty on purpose)'
SELECT COUNT(*) FILTER (WHERE product_id IS NULL) AS unlinked_lines, COUNT(*) AS all_lines
FROM transaction_detail;

\echo '--- 4.6 Sequences behind the highest id (must return no rows)'
SELECT 'transaction' AS tbl, last_value FROM pg_sequences s
WHERE s.sequencename = 'transaction_sequences'
  AND s.last_value < (SELECT MAX(transaction_id) FROM "transaction")
UNION ALL
SELECT 'product', last_value FROM pg_sequences s
WHERE s.sequencename = 'product_sequences'
  AND s.last_value < (SELECT MAX(product_id) FROM product);

\echo '--- 4.7 New rows missing created_at (must return no rows)'
SELECT 'payment_type' AS tbl, COUNT(*) FROM payment_type WHERE created_at IS NULL HAVING COUNT(*) > 0
UNION ALL
SELECT 'payment_method', COUNT(*) FROM payment_method WHERE created_at IS NULL HAVING COUNT(*) > 0
UNION ALL
SELECT 'client_role', COUNT(*) FROM client_role WHERE created_at IS NULL HAVING COUNT(*) > 0
UNION ALL
SELECT 'category', COUNT(*) FROM category WHERE created_at IS NULL HAVING COUNT(*) > 0;

\echo '--- 4.8 Unlinked sale lines, newest first (first 50, for manual review)'
SELECT td.transaction_detail_id, t.transaction_number, td.short_name, td.full_name
FROM transaction_detail td
JOIN "transaction" t ON t.transaction_id = td.transaction_id
WHERE td.product_id IS NULL
ORDER BY t.created_at DESC
LIMIT 50;
