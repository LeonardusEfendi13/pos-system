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
