package com.example.trackingbot.model;

import java.math.BigDecimal;

public record AggTrade(
        BigDecimal price,
        BigDecimal quantity,
        boolean buyerMaker
) {
}
