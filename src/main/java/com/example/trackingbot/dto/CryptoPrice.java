package com.example.trackingbot.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record CryptoPrice(
        String symbol,
        BigDecimal priceUsd,
        BigDecimal changePercent24h,
        BigDecimal totalVolumeUsd,
        BigDecimal high24h,
        BigDecimal low24h,
        Instant lastUpdatedAt
) implements Serializable {
}
