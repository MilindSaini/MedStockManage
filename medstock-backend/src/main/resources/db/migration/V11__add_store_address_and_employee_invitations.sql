ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS address VARCHAR(255);

CREATE TABLE IF NOT EXISTS employee_invitations (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_username VARCHAR(60) NOT NULL,
    invited_email VARCHAR(180) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    can_add BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    can_view_finance BOOLEAN NOT NULL DEFAULT FALSE,
    can_sell BOOLEAN NOT NULL DEFAULT FALSE,
    responded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_employee_invitations_invited_status
    ON employee_invitations (invited_user_id, status);

CREATE INDEX IF NOT EXISTS idx_employee_invitations_store_invited_status
    ON employee_invitations (store_id, invited_user_id, status);
