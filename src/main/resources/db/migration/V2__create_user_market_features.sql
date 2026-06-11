CREATE TABLE watchlist_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES telegram_users(id) ON DELETE CASCADE,
    symbol VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_watchlist_user_symbol UNIQUE (user_id, symbol)
);

CREATE TABLE price_alerts (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES telegram_users(id) ON DELETE CASCADE,
    symbol VARCHAR(30) NOT NULL,
    operator VARCHAR(2) NOT NULL,
    target_price NUMERIC(24, 8) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    triggered_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_price_alerts_active ON price_alerts(active);
CREATE INDEX idx_price_alerts_user_active ON price_alerts(user_id, active);

CREATE TABLE portfolio_positions (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES telegram_users(id) ON DELETE CASCADE,
    side VARCHAR(10) NOT NULL,
    symbol VARCHAR(30) NOT NULL,
    amount NUMERIC(24, 8),
    entry_price NUMERIC(24, 8) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_portfolio_positions_user ON portfolio_positions(user_id);

CREATE TABLE daily_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES telegram_users(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    watch_updates_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_settings_enabled ON daily_settings(enabled);
