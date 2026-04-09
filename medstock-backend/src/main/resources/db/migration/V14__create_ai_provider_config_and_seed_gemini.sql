CREATE TABLE IF NOT EXISTS ai_provider_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    provider_key VARCHAR(40) NOT NULL,
    use_case VARCHAR(60) NOT NULL DEFAULT 'DEFAULT',
    model VARCHAR(120),
    base_url VARCHAR(300) NOT NULL,
    api_key VARCHAR(300),
    api_key_env_var VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    last_test_status VARCHAR(40),
    last_tested_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ai_providers')
       AND NOT EXISTS (SELECT 1 FROM ai_provider_config) THEN
        INSERT INTO ai_provider_config (
            name,
            provider_key,
            use_case,
            model,
            base_url,
            api_key,
            api_key_env_var,
            active,
            last_test_status,
            last_tested_at,
            created_at,
            updated_at
        )
        SELECT
            p.name,
            UPPER(COALESCE(NULLIF(p.name, ''), 'GEMINI')),
            'DEFAULT',
            NULL,
            p.base_url,
            p.api_key,
            CASE
                WHEN UPPER(p.name) = 'OPENAI' THEN 'MEDSTOCK_OPENAI_API_KEY'
                WHEN UPPER(p.name) = 'CLAUDE' THEN 'MEDSTOCK_CLAUDE_API_KEY'
                WHEN UPPER(p.name) = 'OLLAMA' THEN NULL
                ELSE 'MEDSTOCK_GEMINI_API_KEY'
            END,
            p.active,
            p.last_test_status,
            p.last_tested_at,
            p.created_at,
            p.updated_at
        FROM ai_providers p;
    END IF;
END $$;

INSERT INTO ai_provider_config (
    name,
    provider_key,
    use_case,
    model,
    base_url,
    api_key_env_var,
    active
)
SELECT
    'Gemini',
    'GEMINI',
    'DEFAULT',
    'gemini-1.5-flash',
    'https://generativelanguage.googleapis.com',
    'MEDSTOCK_GEMINI_API_KEY',
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM ai_provider_config);
