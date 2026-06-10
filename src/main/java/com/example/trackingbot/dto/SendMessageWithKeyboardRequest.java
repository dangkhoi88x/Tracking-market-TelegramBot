package com.example.trackingbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendMessageWithKeyboardRequest(
        @JsonProperty("chat_id") Long chatId,
        String text,
        @JsonProperty("reply_markup") InlineKeyboardMarkup replyMarkup
) {
}
