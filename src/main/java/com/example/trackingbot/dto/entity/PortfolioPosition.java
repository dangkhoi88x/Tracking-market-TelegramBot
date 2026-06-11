package com.example.trackingbot.dto.entity;

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
