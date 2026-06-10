package com.example.trackingbot.dto;

import java.math.BigDecimal;
import java.util.List;

public record BreakoutSignal(
        String direction,
        boolean confirmed,
        BigDecimal referenceLevel,
        BigDecimal close,
        BigDecimal averageVolume,
        BigDecimal lastVolume,
        BigDecimal volumeRatio,
        BigDecimal volumeDelta,
        List<String> reasons
) {
}
