package com.example.trackingbot.dto;

import java.nio.file.Path;

public record IdeaChartImage(
        Path imagePath,
        String caption,
        String symbol,
        String interval
) {
}
