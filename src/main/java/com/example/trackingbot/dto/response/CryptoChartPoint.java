package com.example.trackingbot.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record CryptoChartPoint(
        Instant time,
        BigDecimal priceUsd
) implements Serializable {
}
