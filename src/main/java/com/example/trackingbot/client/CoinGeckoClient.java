package com.example.trackingbot.client;

import com.example.trackingbot.dto.CryptoPrice;
import com.example.trackingbot.dto.CryptoChartPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class CoinGeckoClient {

    private final RestClient restClient;

    public CoinGeckoClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.coingecko.com/api/v3")
                .build();
    }

    public CryptoPrice getSimplePrice(String coinId, String symbol) {
        CoinGeckoSimplePriceResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/simple/price")
                        .queryParam("ids", coinId)
                        .queryParam("vs_currencies", "usd")
                        .queryParam("include_24hr_change", true)
                        .queryParam("include_last_updated_at", true)
                        .build())
                .retrieve()
                .body(CoinGeckoSimplePriceResponse.class);

        if (response == null || !response.containsKey(coinId)) {
            throw new IllegalStateException("CoinGecko did not return price for " + coinId);
        }

        CoinGeckoPriceData priceData = response.get(coinId);
        return new CryptoPrice(
                symbol,
                priceData.usd(),
                priceData.usd24hChange(),
                Instant.ofEpochSecond(priceData.lastUpdatedAt())
        );
    }

    public List<CryptoChartPoint> getMarketChartRange(String coinId, Instant from, Instant to) {
        CoinGeckoMarketChartResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/{id}/market_chart/range")
                        .queryParam("vs_currency", "usd")
                        .queryParam("from", from.getEpochSecond())
                        .queryParam("to", to.getEpochSecond())
                        .build(coinId))
                .retrieve()
                .body(CoinGeckoMarketChartResponse.class);

        if (response == null || response.prices() == null || response.prices().isEmpty()) {
            throw new IllegalStateException("CoinGecko did not return chart prices for " + coinId);
        }

        return response.prices().stream()
                .filter(point -> point.size() >= 2)
                .map(point -> new CryptoChartPoint(
                        Instant.ofEpochMilli(point.get(0).longValue()),
                        point.get(1)
                ))
                .toList();
    }

    private static class CoinGeckoSimplePriceResponse extends java.util.HashMap<String, CoinGeckoPriceData> {
    }

    private record CoinGeckoPriceData(
            BigDecimal usd,
            @com.fasterxml.jackson.annotation.JsonProperty("usd_24h_change") BigDecimal usd24hChange,
            @com.fasterxml.jackson.annotation.JsonProperty("last_updated_at") Long lastUpdatedAt
    ) {
    }

    private record CoinGeckoMarketChartResponse(
            List<List<BigDecimal>> prices
    ) {
    }
}
