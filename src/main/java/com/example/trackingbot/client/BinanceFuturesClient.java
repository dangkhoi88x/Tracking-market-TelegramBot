package com.example.trackingbot.client;

import com.example.trackingbot.model.BinanceKline;
import com.example.trackingbot.model.AggTrade;
import com.example.trackingbot.model.OrderBookLevel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class BinanceFuturesClient {

    private final RestClient restClient;

    public BinanceFuturesClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://fapi.binance.com")
                .build();
    }

    @Retry(name = "binanceFutures")
    @CircuitBreaker(name = "binanceFutures")
    @RateLimiter(name = "binanceFutures")
    public List<BinanceKline> getKlines(String symbol, String interval, int limit) {
        List<List<Object>> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || response.isEmpty()) {
            throw new IllegalStateException("Binance did not return klines for " + symbol);
        }

        List<BinanceKline> klines = response.stream()
                .filter(row -> row.size() >= 10)
                .map(this::toKline)
                .toList();

        if (klines.isEmpty()) {
            throw new IllegalStateException("Binance did not return valid klines for " + symbol);
        }

        return klines;
    }

    @Retry(name = "binanceFutures")
    @CircuitBreaker(name = "binanceFutures")
    @RateLimiter(name = "binanceFutures")
    public List<OrderBookLevel> getBids(String symbol, int limit) {
        return getDepthSide(symbol, limit, "bids");
    }

    @Retry(name = "binanceFutures")
    @CircuitBreaker(name = "binanceFutures")
    @RateLimiter(name = "binanceFutures")
    public List<OrderBookLevel> getAsks(String symbol, int limit) {
        return getDepthSide(symbol, limit, "asks");
    }

    @Retry(name = "binanceFutures")
    @CircuitBreaker(name = "binanceFutures")
    @RateLimiter(name = "binanceFutures")
    public List<AggTrade> getAggTrades(String symbol, int limit) {
        List<Map<String, Object>> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/aggTrades")
                        .queryParam("symbol", symbol)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null) {
            return List.of();
        }

        return response.stream()
                .map(this::toAggTrade)
                .toList();
    }

    @Retry(name = "binanceFutures")
    @CircuitBreaker(name = "binanceFutures")
    @RateLimiter(name = "binanceFutures")
    public BigDecimal getOpenInterest(String symbol) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/openInterest")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || response.get("openInterest") == null) {
            return BigDecimal.ZERO;
        }

        return toBigDecimal(response.get("openInterest"));
    }

    @Retry(name = "binanceFutures")
    @CircuitBreaker(name = "binanceFutures")
    @RateLimiter(name = "binanceFutures")
    public BigDecimal getFundingRate(String symbol) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/premiumIndex")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || response.get("lastFundingRate") == null) {
            return BigDecimal.ZERO;
        }

        return toBigDecimal(response.get("lastFundingRate"));
    }

    private List<OrderBookLevel> getDepthSide(String symbol, int limit, String side) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fapi/v1/depth")
                        .queryParam("symbol", symbol)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || response.get(side) == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<List<Object>> levels = (List<List<Object>>) response.get(side);
        return levels.stream()
                .filter(level -> level.size() >= 2)
                .map(this::toOrderBookLevel)
                .toList();
    }

    private BinanceKline toKline(List<Object> row) {
        return new BinanceKline(
                Instant.ofEpochMilli(toLong(row.get(0))),
                toBigDecimal(row.get(1)),
                toBigDecimal(row.get(2)),
                toBigDecimal(row.get(3)),
                toBigDecimal(row.get(4)),
                toBigDecimal(row.get(5)),
                toBigDecimal(row.get(9))
        );
    }

    private OrderBookLevel toOrderBookLevel(List<Object> row) {
        return new OrderBookLevel(
                toBigDecimal(row.get(0)),
                toBigDecimal(row.get(1))
        );
    }

    private AggTrade toAggTrade(Map<String, Object> row) {
        return new AggTrade(
                toBigDecimal(row.get("p")),
                toBigDecimal(row.get("q")),
                Boolean.parseBoolean(row.get("m").toString())
        );
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        return new BigDecimal(value.toString());
    }
}
