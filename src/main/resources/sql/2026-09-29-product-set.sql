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
