package com.example.trackingbot.model;

import java.util.List;

public record TrendlineAnalysis(
        List<PivotPoint> pivotHighs,
        List<PivotPoint> pivotLows,
        Trendline uptrend,
        Trendline downtrend,
        String summary
) {
}
