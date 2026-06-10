package com.example.trackingbot.dto;

import java.math.BigDecimal;

public record CryptoAlert(
        String id,
        Long chatId,
        String symbol,
        String operator,
        BigDecimal targetPrice,
        boolean active
) {
}
