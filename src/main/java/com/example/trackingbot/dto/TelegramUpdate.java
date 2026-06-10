package com.example.trackingbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramUpdate(
        @JsonProperty("update_id") Long updateId,
        TelegramMessage message,
        @JsonProperty("callback_query") TelegramCallbackQuery callbackQuery
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

    public record TelegramCallbackQuery(
            String id,
            TelegramMessage message,
            String data
    ) {
    }
}
