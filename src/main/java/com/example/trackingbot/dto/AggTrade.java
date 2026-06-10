package com.example.trackingbot.dto;

import java.math.BigDecimal;

public record AggTrade(
        BigDecimal price,
        BigDecimal quantity,
        boolean buyerMaker
) {
}
