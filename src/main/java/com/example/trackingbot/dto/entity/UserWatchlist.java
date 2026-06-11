package com.example.trackingbot.dto.entity;

import java.util.List;

public record UserWatchlist(
        Long chatId,
        List<String> symbols
) {
}
