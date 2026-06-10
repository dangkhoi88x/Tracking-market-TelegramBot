package com.example.trackingbot.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BinanceKline(
        Instant openTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        BigDecimal takerBuyVolume
) {
    public BigDecimal takerSellVolume() {
        return volume.subtract(takerBuyVolume);
    }

    public BigDecimal volumeDelta() {
        return takerBuyVolume.subtract(takerSellVolume());
    }
}
