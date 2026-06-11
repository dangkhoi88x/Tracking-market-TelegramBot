package com.example.trackingbot.dto.response;

public record SignalScore(
        String symbol,
        String pair,
        String interval,
        int totalScore,
        String bias,
        String risk,
        int trendScore,
        int momentumScore,
        int orderFlowScore,
        int volumeScore,
        int breakoutScore,
        int trendlineScore,
        String summary
) {
}
