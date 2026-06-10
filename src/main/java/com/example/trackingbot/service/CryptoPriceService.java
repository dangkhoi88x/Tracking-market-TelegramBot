package com.example.trackingbot.service;

import com.example.trackingbot.client.CoinGeckoClient;
import com.example.trackingbot.dto.CryptoPrice;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CryptoPriceService {

    private static final String CRYPTO_PRICE_CACHE = "cryptoPrices";

    private static final Map<String, String> SUPPORTED_COINS = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "BNB", "binancecoin",
            "SOL", "solana",
            "XRP", "ripple",
            "DOGE", "dogecoin"
    );

    private final CoinGeckoClient coinGeckoClient;
    private final CacheManager cacheManager;

    public CryptoPriceService(CoinGeckoClient coinGeckoClient, CacheManager cacheManager) {
        this.coinGeckoClient = coinGeckoClient;
        this.cacheManager = cacheManager;
    }

    public String getPriceMessage(String rawSymbol) {
        String symbol = normalizeSymbol(rawSymbol);
        Optional<String> coinId = findCoinId(symbol);

        if (coinId.isEmpty()) {
            return """
                    Minh chua ho tro ma crypto nay.

                    Thu cac lenh:
                    /crypto BTC
                    /crypto ETH
                    /crypto SOL
                    """;
        }

        PriceLookupResult lookupResult = getCachedOrFetch(symbol, coinId.get());
        return formatPriceMessage(lookupResult.price(), lookupResult.fromCache());
    }

    public CryptoPrice getCurrentPrice(String rawSymbol) {
        String symbol = normalizeSymbol(rawSymbol);
        Optional<String> coinId = findCoinId(symbol);

        if (coinId.isEmpty()) {
            throw new IllegalArgumentException("Unsupported crypto symbol: " + symbol);
        }

        return getCachedOrFetch(symbol, coinId.get()).price();
    }

    public boolean isHelpCommand(String rawSymbol) {
        String symbol = normalizeSymbol(rawSymbol);
        return "HELP".equals(symbol) || "?".equals(symbol);
    }

    public String getHelpMessage() {
        return """
                Ma crypto dang ho tro:
                %s

                Vi du:
                /crypto BTC
                /crypto ETH
                """.formatted(getSupportedSymbolsText());
    }

    public String getSupportedSymbolsText() {
        return SUPPORTED_COINS.keySet().stream()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private PriceLookupResult getCachedOrFetch(String symbol, String coinId) {
        Cache cache = cacheManager.getCache(CRYPTO_PRICE_CACHE);
        if (cache == null) {
            CryptoPrice freshPrice = coinGeckoClient.getSimplePrice(coinId, symbol);
            return new PriceLookupResult(freshPrice, false);
        }

        CryptoPrice cachedPrice = cache.get(symbol, CryptoPrice.class);
        if (cachedPrice != null) {
            return new PriceLookupResult(cachedPrice, true);
        }

        CryptoPrice freshPrice = coinGeckoClient.getSimplePrice(coinId, symbol);
        cache.put(symbol, freshPrice);
        return new PriceLookupResult(freshPrice, false);
    }

    public Optional<String> findCoinId(String symbol) {
        if (symbol.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(SUPPORTED_COINS.get(symbol));
    }

    public String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null) {
            return "";
        }

        return rawSymbol.trim().toUpperCase(Locale.ROOT);
    }

    private String formatPriceMessage(CryptoPrice price, boolean fromCache) {
        return """
                %s
                Price %s
                24h %s%% Vol %s
                High %s (%s%%)
                Low %s (%s%%)
                """.formatted(
                price.symbol(),
                formatMoney(price.priceUsd()),
                formatSignedPercent(price.changePercent24h()),
                formatCompactMoney(price.totalVolumeUsd()),
                formatMoney(price.high24h()),
                formatDistanceFromCurrent(price.priceUsd(), price.high24h()),
                formatMoney(price.low24h()),
                formatDistanceFromCurrent(price.priceUsd(), price.low24h())
        );
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }

        return "%,.2f".formatted(value);
    }

    private String formatCompactMoney(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }

        BigDecimal billion = BigDecimal.valueOf(1_000_000_000L);
        BigDecimal million = BigDecimal.valueOf(1_000_000L);

        if (value.compareTo(billion) >= 0) {
            return value.divide(billion, 2, RoundingMode.HALF_UP) + "B";
        }

        if (value.compareTo(million) >= 0) {
            return value.divide(million, 2, RoundingMode.HALF_UP) + "M";
        }

        return "%,.0f".formatted(value);
    }

    private String formatDistanceFromCurrent(BigDecimal currentPrice, BigDecimal targetPrice) {
        if (currentPrice == null || targetPrice == null || currentPrice.signum() == 0) {
            return "N/A";
        }

        BigDecimal distance = currentPrice.subtract(targetPrice)
                .divide(currentPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return formatSignedPercent(distance);
    }

    private String formatSignedPercent(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }

        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() > 0) {
            return "+" + rounded;
        }

        return rounded.toString();
    }

    private record PriceLookupResult(
            CryptoPrice price,
            boolean fromCache
    ) {
    }
}
