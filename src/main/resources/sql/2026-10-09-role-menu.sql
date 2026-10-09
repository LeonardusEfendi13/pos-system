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
