package com.example.trackingbot.dto;

import java.math.BigDecimal;

public record OrderBookLevel(
        BigDecimal price,
        BigDecimal quantity
) {
}
