CREATE TABLE IF NOT EXISTS medicine_master (
    id BIGSERIAL PRIMARY KEY,
    barcode VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(240) NOT NULL,
    generic_name VARCHAR(240),
    manufacturer VARCHAR(200),
    category VARCHAR(120),
    unit VARCHAR(40) NOT NULL DEFAULT 'pcs',
    source VARCHAR(40) NOT NULL DEFAULT 'SEED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Bulk-seed 10,000+ placeholder master rows to support barcode flow bootstrapping.
INSERT INTO medicine_master (
    barcode,
    name,
    generic_name,
    manufacturer,
    category,
    unit,
    source
)
SELECT
    LPAD(gs::text, 12, '0') AS barcode,
    'Drug ' || gs,
    'Generic ' || gs,
    'Manufacturer ' || ((gs % 250) + 1),
    CASE
        WHEN gs % 7 = 0 THEN 'Analgesic'
        WHEN gs % 7 = 1 THEN 'Antibiotic'
        WHEN gs % 7 = 2 THEN 'Antacid'
        WHEN gs % 7 = 3 THEN 'Antihistamine'
        WHEN gs % 7 = 4 THEN 'Vitamin'
        WHEN gs % 7 = 5 THEN 'Cardiac Care'
        ELSE 'Respiratory'
    END,
    'pcs',
    'SEED'
FROM generate_series(1, 10050) AS gs
WHERE NOT EXISTS (SELECT 1 FROM medicine_master LIMIT 1);
