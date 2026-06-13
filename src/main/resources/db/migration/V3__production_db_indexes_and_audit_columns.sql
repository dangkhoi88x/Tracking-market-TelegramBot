ALTER TABLE watchlist_items
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE price_alerts
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE portfolio_positions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_watchlist_items_symbol
    ON watchlist_items(symbol);

CREATE INDEX IF NOT EXISTS idx_price_alerts_symbol_active
    ON price_alerts(symbol, active);

CREATE INDEX IF NOT EXISTS idx_price_alerts_user_symbol_active
    ON price_alerts(user_id, symbol, active);

CREATE INDEX IF NOT EXISTS idx_price_alerts_active_symbol_created_at
    ON price_alerts(active, symbol, created_at);

CREATE INDEX IF NOT EXISTS idx_price_alerts_inactive_updated_at
    ON price_alerts(updated_at)
    WHERE active = FALSE;

CREATE INDEX IF NOT EXISTS idx_portfolio_positions_symbol
    ON portfolio_positions(symbol);

CREATE INDEX IF NOT EXISTS idx_portfolio_positions_user_symbol_created_at
    ON portfolio_positions(user_id, symbol, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_daily_settings_enabled_watch_updates
    ON daily_settings(enabled, watch_updates_enabled);
