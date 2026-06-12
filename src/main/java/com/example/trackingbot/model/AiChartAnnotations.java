package com.example.trackingbot.model;

import java.math.BigDecimal;

public record AiChartAnnotations(
        BigDecimal rangeLow,
        BigDecimal rangeHigh,
        BigDecimal currentPrice,
        BigDecimal ema20,
        BigDecimal ema50,
        BigDecimal bullishTrigger,
        BigDecimal bearishTrigger,
        BigDecimal invalidationBullish,
        BigDecimal resistanceZoneLow,
        BigDecimal resistanceZoneHigh,
        BigDecimal supportZoneLow,
        BigDecimal supportZoneHigh,
        String bias,
        int confidence
) {
}
