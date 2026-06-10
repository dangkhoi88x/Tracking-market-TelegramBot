package com.example.trackingbot.dto;

import java.io.Serializable;

public record CryptoChartImage(
        String imageUrl,
        String caption
) implements Serializable {
}
