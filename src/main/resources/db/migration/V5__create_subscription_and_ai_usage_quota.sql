ALTER TABLE telegram_users
    ADD COLUMN IF NOT EXISTS plan VARCHAR(20) NOT NULL DEFAULT 'FREE';

CREATE TABLE IF NOT EXISTS ai_usage_quotas (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES telegram_users(id) ON DELETE CASCADE,
    feature VARCHAR(50) NOT NULL,
    usage_date DATE NOT NULL,
    used_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_usage_user_feature_date UNIQUE (user_id, feature, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_telegram_users_plan
    ON telegram_users(plan);

CREATE INDEX IF NOT EXISTS idx_ai_usage_quotas_user_feature_date
    ON ai_usage_quotas(user_id, feature, usage_date DESC);
