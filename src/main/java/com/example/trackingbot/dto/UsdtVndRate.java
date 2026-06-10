package com.example.trackingbot.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record UsdtVndRate(
        BigDecimal priceVnd,
        String source,
        Instant updatedAt
) implements Serializable {
}
