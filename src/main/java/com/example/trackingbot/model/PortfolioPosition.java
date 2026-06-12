package com.example.trackingbot.model;

import java.math.BigDecimal;

public record PortfolioPosition(
        String id,
        Long chatId,
        String side,
        String symbol,
        BigDecimal amount,
        BigDecimal entryPrice
) {
}
