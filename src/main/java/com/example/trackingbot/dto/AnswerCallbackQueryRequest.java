package com.example.trackingbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AnswerCallbackQueryRequest(
        @JsonProperty("callback_query_id") String callbackQueryId,
        String text
) {
}
