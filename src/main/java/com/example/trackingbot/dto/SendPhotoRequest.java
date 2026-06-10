package com.example.trackingbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendPhotoRequest(
        @JsonProperty("chat_id") Long chatId,
        String photo,
        String caption
) {
}
