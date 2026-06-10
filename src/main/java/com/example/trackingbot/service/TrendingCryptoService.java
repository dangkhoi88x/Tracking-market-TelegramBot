package com.example.trackingbot.service;

import com.example.trackingbot.client.CoinGeckoClient;
import com.example.trackingbot.dto.TrendingCrypto;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TrendingCryptoService {

    private static final String TRENDING_CACHE = "trendingCryptos";
    private static final String TRENDING_TOP_10_KEY = "top10";

    private final CoinGeckoClient coinGeckoClient;
    private final CacheManager cacheManager;

    public TrendingCryptoService(CoinGeckoClient coinGeckoClient, CacheManager cacheManager) {
        this.coinGeckoClient = coinGeckoClient;
        this.cacheManager = cacheManager;
    }

    public String getTopTrendingMessage() {
        List<TrendingCrypto> cryptos = getTopTrendingCryptos();
        if (cryptos.isEmpty()) {
            return "Tam thoi khong co du lieu trending crypto.";
        }

        StringBuilder message = new StringBuilder("Top 10 crypto dang trending:\n\n");
        for (int i = 0; i < cryptos.size(); i++) {
            TrendingCrypto crypto = cryptos.get(i);
            message.append("%d. %s (%s)\n".formatted(i + 1, crypto.name(), crypto.symbol()));
            message.append("Gia: %s USD | 24h: %s%%\n".formatted(
                    formatMoney(crypto.priceUsd()),
                    formatSignedPercent(crypto.changePercent24h())
            ));
            message.append("Volume: %s USD\n\n".formatted(formatCompactMoney(crypto.totalVolumeUsd())));
        }

        return message.toString();
    }

    @SuppressWarnings("unchecked")
    private List<TrendingCrypto> getTopTrendingCryptos() {
        Cache cache = cacheManager.getCache(TRENDING_CACHE);
        if (cache != null) {
            List<TrendingCrypto> cachedTrending = cache.get(TRENDING_TOP_10_KEY, List.class);
            if (cachedTrending != null) {
                return cachedTrending;
            }
        }

        List<TrendingCrypto> freshTrending = coinGeckoClient.getTrendingCryptos(10);
        if (cache != null) {
            cache.put(TRENDING_TOP_10_KEY, freshTrending);
        }

        return freshTrending;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }

        if (value.compareTo(BigDecimal.ONE) < 0) {
            return value.setScale(6, RoundingMode.HALF_UP).toPlainString();
        }

        return "%,.2f".formatted(value);
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
}
