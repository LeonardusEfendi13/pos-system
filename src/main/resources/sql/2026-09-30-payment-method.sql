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
