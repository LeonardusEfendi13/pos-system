-- Bonus is a fixed monthly amount per staff. Like uang makan, it is part of the
-- salary earned per day (absences deduct it); insentif stays separate
ALTER TABLE staff ADD COLUMN IF NOT EXISTS bonus NUMERIC NOT NULL DEFAULT 0;
ALTER TABLE payroll ADD COLUMN IF NOT EXISTS bonus NUMERIC NOT NULL DEFAULT 0;
