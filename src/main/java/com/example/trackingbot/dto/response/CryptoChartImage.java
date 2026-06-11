package com.example.trackingbot.dto.response;

import java.io.Serializable;

public record CryptoChartImage(
        String imageUrl,
        String caption
) implements Serializable {
}
