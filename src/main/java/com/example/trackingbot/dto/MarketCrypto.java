package com.example.trackingbot.dto;

import java.math.BigDecimal;

public record MarketCrypto(
        String id,
        String symbol,
        String name,
        BigDecimal priceUsd,
        BigDecimal changePercent24h,
        BigDecimal totalVolumeUsd
) {
}
