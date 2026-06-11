package com.example.trackingbot.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record UsdtVndRate(
        BigDecimal priceVnd,
        String source,
        Instant updatedAt
) implements Serializable {
}
