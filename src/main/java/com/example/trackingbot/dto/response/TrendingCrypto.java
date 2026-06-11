package com.example.trackingbot.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public record TrendingCrypto(
        String id,
        String symbol,
        String name,
        BigDecimal priceUsd,
        BigDecimal changePercent24h,
        BigDecimal totalVolumeUsd
) implements Serializable {
}
