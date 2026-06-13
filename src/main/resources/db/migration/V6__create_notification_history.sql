CREATE TABLE IF NOT EXISTS notification_history (
    id BIGSERIAL PRIMARY KEY,
    notification_id VARCHAR(36) NOT NULL UNIQUE,
    chat_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    text TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_history_chat_id_sent_at
    ON notification_history(chat_id, sent_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_history_type_sent_at
    ON notification_history(type, sent_at DESC);
