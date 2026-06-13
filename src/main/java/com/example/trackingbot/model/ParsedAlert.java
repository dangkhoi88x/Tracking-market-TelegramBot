package com.example.trackingbot.model;

import java.math.BigDecimal;

public record ParsedAlert(
        String symbol,
        String operator,
        BigDecimal targetPrice
) {
}
