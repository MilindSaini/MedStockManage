CREATE UNIQUE INDEX IF NOT EXISTS ux_users_phone
    ON users (phone)
    WHERE phone IS NOT NULL;
