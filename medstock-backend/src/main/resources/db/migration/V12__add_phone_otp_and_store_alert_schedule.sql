ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS expiry_alert_time TIME NOT NULL DEFAULT '08:00:00',
    ADD COLUMN IF NOT EXISTS low_stock_alert_time TIME NOT NULL DEFAULT '08:30:00',
    ADD COLUMN IF NOT EXISTS out_of_stock_alert_time TIME NOT NULL DEFAULT '09:00:00',
    ADD COLUMN IF NOT EXISTS batch_promotion_time TIME NOT NULL DEFAULT '06:00:00';

CREATE TABLE IF NOT EXISTS phone_verification_otps (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone VARCHAR(30) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_phone_verification_otps_user_phone
    ON phone_verification_otps (user_id, phone, created_at DESC);
