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
