package com.example.trackingbot.dto.response;

import java.math.BigDecimal;

public record PivotPoint(
        int index,
        long time,
        BigDecimal price,
        String type
) {
}
