package com.example.trackingbot.model;

import java.util.List;

public record UserWatchlist(
        Long chatId,
        List<String> symbols
) {
}
