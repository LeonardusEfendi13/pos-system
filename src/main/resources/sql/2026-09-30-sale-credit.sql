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
