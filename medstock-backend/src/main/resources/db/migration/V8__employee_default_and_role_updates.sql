ALTER TABLE users
    ALTER COLUMN full_name DROP NOT NULL,
    ALTER COLUMN password_hash DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users ((LOWER(email)));

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username_lower
    ON users ((LOWER(username)))
    WHERE username IS NOT NULL;
