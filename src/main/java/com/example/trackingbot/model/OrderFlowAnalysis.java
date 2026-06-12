package com.example.trackingbot.model;

import java.math.BigDecimal;

public record OrderFlowAnalysis(
        BigDecimal bidVolume,
        BigDecimal askVolume,
        BigDecimal bidDominance,
        BigDecimal buyTradeVolume,
        BigDecimal sellTradeVolume,
        BigDecimal tradeBuyRatio,
        BigDecimal openInterest,
        BigDecimal fundingRate,
        String bookPressure,
        String tradePressure,
        String openInterestState,
        String summary
) {
}
