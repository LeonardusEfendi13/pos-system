-- Jabatan is now Probation / Junior / Senior; Kasir and Entry staff move to Probation
UPDATE staff
SET jabatan = 'PROBATION'
WHERE jabatan IN ('KASIR', 'KARYAWAN_ENTRY');

ALTER TABLE staff DROP CONSTRAINT IF EXISTS staff_jabatan_check;
ALTER TABLE staff ADD CONSTRAINT staff_jabatan_check CHECK (
    jabatan IN ('PROBATION', 'KARYAWAN_JUNIOR', 'KARYAWAN_SENIOR')
);

-- Payroll is calculated live; a row is only written when a month is marked paid
ALTER TABLE payroll ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITHOUT TIME ZONE;
