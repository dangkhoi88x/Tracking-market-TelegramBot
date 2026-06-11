package com.example.trackingbot.dto.response;

import java.nio.file.Path;

public record IdeaChartImage(
        Path imagePath,
        String caption,
        String symbol,
        String interval
) {
}
