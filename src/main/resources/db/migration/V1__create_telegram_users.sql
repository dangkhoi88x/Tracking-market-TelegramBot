CREATE TABLE telegram_users (
                                id BIGSERIAL PRIMARY KEY,
                                chat_id BIGINT NOT NULL UNIQUE,
                                username VARCHAR(255),
                                first_name VARCHAR(255),
                                last_name VARCHAR(255),
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);