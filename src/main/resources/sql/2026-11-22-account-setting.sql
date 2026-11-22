-- Per-account UI preferences. One row per account, created lazily on the first
-- change: an account without a row uses the default (font_weight_level = 2).
--
-- Safe to run more than once. No data is copied or changed in existing tables,
-- and no FK to account (account_id has no PK in older dumps, same as the other
-- tables here that hold ids without constraints).
--
-- Run:      psql -d pos_anugrah_migrated -f 2026-11-22-account-setting.sql
-- Verify:   SELECT * FROM account_setting;   -- empty until someone changes a level
-- Rollback: DROP TABLE account_setting;      -- only drops preferences, accounts stay

CREATE TABLE IF NOT EXISTS account_setting (
    account_id NUMERIC PRIMARY KEY,
    font_weight_level SMALLINT NOT NULL DEFAULT 2,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT account_setting_font_weight_level_check CHECK (
        font_weight_level BETWEEN 1 AND 4
    )
);
