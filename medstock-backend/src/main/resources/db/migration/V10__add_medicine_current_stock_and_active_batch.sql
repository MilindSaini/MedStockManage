ALTER TABLE medicines
    ADD COLUMN IF NOT EXISTS active_batch_id BIGINT REFERENCES batch_groups(id) ON DELETE SET NULL;

ALTER TABLE medicines
    ADD COLUMN IF NOT EXISTS current_stock INTEGER GENERATED ALWAYS AS (GREATEST(quantity_available - quantity_sold, 0)) STORED;

CREATE INDEX IF NOT EXISTS idx_medicines_current_stock ON medicines(current_stock);
CREATE INDEX IF NOT EXISTS idx_medicines_active_batch_id ON medicines(active_batch_id);
