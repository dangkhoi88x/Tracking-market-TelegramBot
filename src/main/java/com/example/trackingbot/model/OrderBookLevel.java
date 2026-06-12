package com.example.trackingbot.model;

import java.math.BigDecimal;

public record OrderBookLevel(
        BigDecimal price,
        BigDecimal quantity
) {
}
