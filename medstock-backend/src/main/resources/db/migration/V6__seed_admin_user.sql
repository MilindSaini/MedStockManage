INSERT INTO users (
    store_id,
    username,
    email,
    full_name,
    phone,
    password_hash,
    role,
    is_active,
    email_verified
)
SELECT
    NULL,
    'admin',
    'admin@medstock.local',
    'MedStock Admin',
    NULL,
    '${adminSeedPassword}',
    'ADMIN',
    TRUE,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);
