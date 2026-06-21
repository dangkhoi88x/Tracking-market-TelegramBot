package com.example.trackingbot.client;

import com.example.trackingbot.dto.response.UsdtVndRate;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Component
public class BinanceP2PClient {

    private static final int ROWS = 5;

    private final RestClient restClient;

    // Creates the HTTP client for Binance P2P API calls.
    public BinanceP2PClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://p2p.binance.com")
                .build();
    }

    // Gets the average USDT/VND price from the first P2P ads.
    @Retry(name = "binanceP2P")
    @CircuitBreaker(name = "binanceP2P")
    @RateLimiter(name = "binanceP2P")
    public UsdtVndRate getUsdtVndRate() {
        BinanceP2PResponse response = restClient.post()
                .uri("/bapi/c2c/v2/friendly/c2c/adv/search")
                .body(new BinanceP2PRequest(
                        1,
                        ROWS,
                        List.of(),
                        "USDT",
                        "VND",
                        "BUY"
                ))
                .retrieve()
                .body(BinanceP2PResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("Binance P2P did not return USDT/VND prices");
        }

        BigDecimal total = response.data().stream()
                .map(item -> item.adv().price())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(BigDecimal.valueOf(response.data().size()), 0, RoundingMode.HALF_UP);

        return new UsdtVndRate(average, "Binance P2P USDT/VND", Instant.now());
    }

    private record BinanceP2PRequest(
            Integer page,
            Integer rows,
            List<String> payTypes,
            String asset,
            String fiat,
            String tradeType
    ) {
    }

    private record BinanceP2PResponse(
            List<BinanceP2PItem> data
    ) {
    }

    private record BinanceP2PItem(
            BinanceP2PAdv adv
    ) {
    }

    private record BinanceP2PAdv(
            @JsonProperty("price") BigDecimal price
    ) {
    }
}
