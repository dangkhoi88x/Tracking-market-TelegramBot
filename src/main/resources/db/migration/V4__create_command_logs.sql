CREATE TABLE command_logs (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    command VARCHAR(100) NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    duration_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_command_logs_chat_id_created_at
    ON command_logs(chat_id, created_at DESC);

CREATE INDEX idx_command_logs_command_created_at
    ON command_logs(command, created_at DESC);

CREATE INDEX idx_command_logs_success_created_at
    ON command_logs(success, created_at DESC);

CREATE INDEX idx_command_logs_created_at
    ON command_logs(created_at DESC);
