ALTER TABLE users
    ADD COLUMN preferred_units VARCHAR(2) NOT NULL DEFAULT 'km';

-- Valid values: 'km' or 'mi'
COMMENT ON COLUMN users.preferred_units IS 'User preferred distance unit: km or mi';