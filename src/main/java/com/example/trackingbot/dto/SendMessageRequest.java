package com.example.trackingbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendMessageRequest(
        @JsonProperty("chat_id") Long chatId,
        String text
) {
}
