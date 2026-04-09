CREATE TABLE IF NOT EXISTS ai_providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    base_url VARCHAR(300) NOT NULL,
    api_key VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    last_test_status VARCHAR(40),
    last_tested_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE activity_logs
    ALTER COLUMN store_id DROP NOT NULL;

INSERT INTO ai_providers (name, base_url, api_key, active)
SELECT 'OpenAI', 'https://api.openai.com/v1/models', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM ai_providers);
