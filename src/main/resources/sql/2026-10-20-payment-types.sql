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
