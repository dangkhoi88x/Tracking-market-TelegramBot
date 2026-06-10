package com.example.trackingbot.client;

import com.example.trackingbot.dto.CryptoPrice;
import com.example.trackingbot.dto.CryptoChartPoint;
import com.example.trackingbot.dto.MarketCrypto;
import com.example.trackingbot.dto.TrendingCrypto;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CoinGeckoClient {

    private final RestClient restClient;

    public CoinGeckoClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.coingecko.com/api/v3")
                .build();
    }

    public CryptoPrice getSimplePrice(String coinId, String symbol) {
        List<CoinGeckoMarketData> marketData = getMarketData(List.of(coinId));
        if (marketData.isEmpty()) {
            throw new IllegalStateException("CoinGecko did not return market data for " + coinId);
        }

        CoinGeckoMarketData priceData = marketData.get(0);
        return new CryptoPrice(
                symbol,
                priceData.currentPrice(),
                priceData.priceChangePercentage24h(),
                priceData.totalVolume(),
                priceData.high24h(),
                priceData.low24h(),
                Instant.now()
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

    public List<TrendingCrypto> getTrendingCryptos(int limit) {
        CoinGeckoTrendingResponse trendingResponse = restClient.get()
                .uri("/search/trending")
                .retrieve()
                .body(CoinGeckoTrendingResponse.class);

        if (trendingResponse == null || trendingResponse.coins() == null || trendingResponse.coins().isEmpty()) {
            throw new IllegalStateException("CoinGecko did not return trending coins");
        }

        List<String> coinIds = trendingResponse.coins().stream()
                .limit(limit)
                .map(trendingCoin -> trendingCoin.item().id())
                .toList();

        Map<String, CoinGeckoMarketData> marketDataById = getMarketData(coinIds).stream()
                .collect(Collectors.toMap(CoinGeckoMarketData::id, marketData -> marketData));

        return coinIds.stream()
                .map(marketDataById::get)
                .filter(marketData -> marketData != null)
                .map(marketData -> new TrendingCrypto(
                        marketData.id(),
                        marketData.symbol().toUpperCase(),
                        marketData.name(),
                        marketData.currentPrice(),
                        marketData.priceChangePercentage24h(),
                        marketData.totalVolume()
                ))
                .toList();
    }

    public List<MarketCrypto> getTopMarketCryptos(int limit) {
        return getMarketDataByMarketCap(limit).stream()
                .map(this::toMarketCrypto)
                .toList();
    }

    public List<MarketCrypto> getMarketCryptosByMarketCap(int limit) {
        return getMarketDataByMarketCap(limit).stream()
                .map(this::toMarketCrypto)
                .toList();
    }

    private List<CoinGeckoMarketData> getMarketData(List<String> coinIds) {
        if (coinIds.isEmpty()) {
            return List.of();
        }

        CoinGeckoMarketData[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/markets")
                        .queryParam("vs_currency", "usd")
                        .queryParam("ids", String.join(",", coinIds))
                        .queryParam("order", "market_cap_desc")
                        .queryParam("per_page", coinIds.size())
                        .queryParam("page", 1)
                        .queryParam("sparkline", false)
                        .build())
                .retrieve()
                .body(CoinGeckoMarketData[].class);

        if (response == null) {
            throw new IllegalStateException("CoinGecko did not return market data");
        }

        return List.of(response);
    }

    private List<CoinGeckoMarketData> getMarketDataByMarketCap(int limit) {
        CoinGeckoMarketData[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/markets")
                        .queryParam("vs_currency", "usd")
                        .queryParam("order", "market_cap_desc")
                        .queryParam("per_page", limit)
                        .queryParam("page", 1)
                        .queryParam("sparkline", false)
                        .queryParam("price_change_percentage", "24h")
                        .build())
                .retrieve()
                .body(CoinGeckoMarketData[].class);

        if (response == null) {
            throw new IllegalStateException("CoinGecko did not return market data");
        }

        return List.of(response);
    }

    private MarketCrypto toMarketCrypto(CoinGeckoMarketData marketData) {
        return new MarketCrypto(
                marketData.id(),
                marketData.symbol().toUpperCase(),
                marketData.name(),
                marketData.currentPrice(),
                marketData.priceChangePercentage24h(),
                marketData.totalVolume()
        );
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

    private record CoinGeckoTrendingResponse(
            List<CoinGeckoTrendingCoin> coins
    ) {
    }

    private record CoinGeckoTrendingCoin(
            CoinGeckoTrendingItem item
    ) {
    }

    private record CoinGeckoTrendingItem(
            String id
    ) {
    }

    private record CoinGeckoMarketData(
            String id,
            String symbol,
            String name,
            @JsonProperty("current_price") BigDecimal currentPrice,
            @JsonProperty("price_change_percentage_24h") BigDecimal priceChangePercentage24h,
            @JsonProperty("high_24h") BigDecimal high24h,
            @JsonProperty("low_24h") BigDecimal low24h,
            @JsonProperty("total_volume") BigDecimal totalVolume
    ) {
    }
}
