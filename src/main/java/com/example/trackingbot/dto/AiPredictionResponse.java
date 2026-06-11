package com.example.trackingbot.dto;

import java.util.List;

public record AiPredictionResponse(
        String bias,
        int confidence,
        String riskLevel,
        String marketRegime,
        String executiveSummary,
        List<String> evidence,
        String bullishScenario,
        String bearishScenario,
        String invalidation,
        String keyLevels,
        String riskManagement,
        List<String> watchlistTriggers
) {
}
