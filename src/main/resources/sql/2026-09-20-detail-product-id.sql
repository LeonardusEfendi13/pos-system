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
