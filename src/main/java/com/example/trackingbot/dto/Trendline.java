package com.example.trackingbot.dto;

import java.math.BigDecimal;

public record Trendline(
        String type,
        PivotPoint first,
        PivotPoint second,
        long extendedTime,
        BigDecimal extendedPrice,
        int touches,
        boolean active
) {
}
