package com.example.trackingbot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record InlineKeyboardMarkup(
        //danh sách các hàng button
        //List ngoài = nhiều hàng
        //List trong = nhiều button trong một hàng

        @JsonProperty("inline_keyboard") List<List<InlineKeyboardButton>> inlineKeyboard
) {
}
