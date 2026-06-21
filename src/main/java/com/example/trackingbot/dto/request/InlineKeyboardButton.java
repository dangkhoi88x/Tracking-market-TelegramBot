package com.example.trackingbot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InlineKeyboardButton(
        // button gửi kèm message  có 2 phần : text dể user thấy và dữ liệu gửi về backend
        String text,
        @JsonProperty("callback_data") String callbackData
) {
}
