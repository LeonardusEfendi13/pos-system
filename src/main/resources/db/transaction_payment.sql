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
