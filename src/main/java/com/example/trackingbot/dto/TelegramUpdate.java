package com.example.trackingbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramUpdate(
        @JsonProperty("update_id") Long updateId,
        TelegramMessage message
) {
    public record TelegramMessage(
            TelegramChat chat,
            String text
    ) {
    }

    public record TelegramChat(
            Long id
    ) {
    }
}
