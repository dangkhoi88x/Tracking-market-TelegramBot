package com.example.trackingbot.dto;

import java.math.BigDecimal;
import java.util.List;

public record TechnicalAnalysis(
        String symbol,
        String pair,
        String interval,
        List<BinanceKline> klines,
        List<BigDecimal> ema20,
        List<BigDecimal> ema50,
        List<BigDecimal> cumulativeVolumeDelta,
        BigDecimal lastClose,
        BigDecimal support,
        BigDecimal resistance,
        BigDecimal priorSupport,
        BigDecimal priorResistance,
        BigDecimal averageVolume20,
        BigDecimal lastBuyVolume,
        BigDecimal lastSellVolume,
        BigDecimal lastVolumeDelta,
        BigDecimal totalVolumeDelta,
        String bias,
        BreakoutSignal breakoutSignal,
        TrendlineAnalysis trendlineAnalysis
) {
}
