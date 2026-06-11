package com.example.trackingbot.service.crypto;

import com.example.trackingbot.client.BinanceP2PClient;
import com.example.trackingbot.dto.response.UsdtVndRate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class UsdtRateService {

    private static final String USDT_RATE_CACHE = "usdtRates";
    private static final String USDT_VND_KEY = "USDT_VND";

    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final BinanceP2PClient binanceP2PClient;
    private final CacheManager cacheManager;

    public UsdtRateService(BinanceP2PClient binanceP2PClient, CacheManager cacheManager) {
        this.binanceP2PClient = binanceP2PClient;
        this.cacheManager = cacheManager;
    }

    public UsdtVndRate getUsdtVndRate() {
        Cache cache = cacheManager.getCache(USDT_RATE_CACHE);
        if (cache != null) {
            UsdtVndRate cachedRate = cache.get(USDT_VND_KEY, UsdtVndRate.class);
            if (cachedRate != null) {
                return cachedRate;
            }
        }

        UsdtVndRate freshRate = binanceP2PClient.getUsdtVndRate();
        if (cache != null) {
            cache.put(USDT_VND_KEY, freshRate);
        }

        return freshRate;
    }

    public String getUsdtMessage() {
        UsdtVndRate rate = getUsdtVndRate();
        return """
                USDT/VND
                1 USDT ~= %s VND
                1 USD ~= %s VND

                Nguon: %s
                Cap nhat: %s
                """.formatted(
                formatVnd(rate.priceVnd()),
                formatVnd(rate.priceVnd()),
                rate.source(),
                UPDATED_AT_FORMATTER.format(rate.updatedAt())
        );
    }

    public String formatVnd(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }

        return "%,.0f".formatted(value.setScale(0, RoundingMode.HALF_UP));
    }
}
