package com.example.trackingbot.dto;

import java.util.List;

public record UserWatchlist(
        Long chatId,
        List<String> symbols
) {
}
