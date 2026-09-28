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
