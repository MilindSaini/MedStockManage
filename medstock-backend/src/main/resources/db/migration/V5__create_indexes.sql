CREATE INDEX IF NOT EXISTS idx_users_store_id ON users(store_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

CREATE INDEX IF NOT EXISTS idx_medicines_store_id ON medicines(store_id);
CREATE INDEX IF NOT EXISTS idx_medicines_store_name ON medicines(store_id, name);
CREATE INDEX IF NOT EXISTS idx_medicines_store_deleted ON medicines(store_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_medicines_expiry_date ON medicines(expiry_date);

CREATE INDEX IF NOT EXISTS idx_batch_groups_store_medicine ON batch_groups(store_id, medicine_id);
CREATE INDEX IF NOT EXISTS idx_stock_transactions_store_medicine ON stock_transactions(store_id, medicine_id);
CREATE INDEX IF NOT EXISTS idx_stock_transactions_created_at ON stock_transactions(created_at);

CREATE INDEX IF NOT EXISTS idx_activity_logs_store_created_at ON activity_logs(store_id, created_at);
CREATE INDEX IF NOT EXISTS idx_alert_logs_store_read ON alert_logs(store_id, is_read);

CREATE INDEX IF NOT EXISTS idx_subscriptions_store_status ON subscriptions(store_id, status);
CREATE INDEX IF NOT EXISTS idx_payment_events_store_created_at ON payment_events(store_id, created_at);
CREATE INDEX IF NOT EXISTS idx_email_tokens_user ON email_verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_tokens_expires_at ON email_verification_tokens(expires_at);
