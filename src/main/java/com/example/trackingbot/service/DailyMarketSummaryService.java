package com.example.trackingbot.service;

import com.example.trackingbot.client.CoinGeckoClient;
import com.example.trackingbot.dto.MarketCrypto;
import com.example.trackingbot.dto.TrendingCrypto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class DailyMarketSummaryService {

    private static final String DAILY_SUBSCRIBERS_KEY = "daily_market_summary:subscribers";
    private static final int TOP_LIMIT = 10;
    private static final int MOVER_UNIVERSE_LIMIT = 250;

    private final StringRedisTemplate redisTemplate;
    private final CoinGeckoClient coinGeckoClient;

    public DailyMarketSummaryService(StringRedisTemplate redisTemplate, CoinGeckoClient coinGeckoClient) {
        this.redisTemplate = redisTemplate;
        this.coinGeckoClient = coinGeckoClient;
    }

    public String enableDailySummary(Long chatId) {
        redisTemplate.opsForSet().add(DAILY_SUBSCRIBERS_KEY, chatId.toString());
        return """
                Da bat Daily Market Summary.

                Moi sang bot se gui:
                top 10 token, top 10 trending, top 10 gainers, top 10 losers.
                """;
    }

    public String disableDailySummary(Long chatId) {
        redisTemplate.opsForSet().remove(DAILY_SUBSCRIBERS_KEY, chatId.toString());
        return "Da tat Daily Market Summary.";
    }

    public List<Long> getSubscriberChatIds() {
        Set<String> chatIds = redisTemplate.opsForSet().members(DAILY_SUBSCRIBERS_KEY);
        return Optional.ofNullable(chatIds)
                .orElse(Collections.emptySet())
                .stream()
                .map(this::parseChatId)
                .flatMap(Optional::stream)
                .toList();
    }

    public String buildDailySummaryMessage() {
        List<MarketCrypto> topTokens = coinGeckoClient.getTopMarketCryptos(TOP_LIMIT);
        List<TrendingCrypto> trending = coinGeckoClient.getTrendingCryptos(TOP_LIMIT);
        List<MarketCrypto> moverUniverse = coinGeckoClient.getMarketCryptosByMarketCap(MOVER_UNIVERSE_LIMIT);

        List<MarketCrypto> gainers = moverUniverse.stream()
                .filter(crypto -> crypto.changePercent24h() != null)
                .sorted(Comparator.comparing(MarketCrypto::changePercent24h).reversed())
                .limit(TOP_LIMIT)
                .toList();

        List<MarketCrypto> losers = moverUniverse.stream()
                .filter(crypto -> crypto.changePercent24h() != null)
                .sorted(Comparator.comparing(MarketCrypto::changePercent24h))
                .limit(TOP_LIMIT)
                .toList();

        return """
                Daily Market Summary

                Top 10 token:
                %s

                Top 10 trending:
                %s

                Top 10 gainers 24h:
                %s

                Top 10 losers 24h:
                %s
                """.formatted(
                formatMarketList(topTokens),
                formatTrendingList(trending),
                formatMarketList(gainers),
                formatMarketList(losers)
        );
    }

    public String getHelpMessage() {
        return """
                Daily Market Summary:
                /daily_on - bat gui summary moi sang
                /daily_off - tat gui summary moi sang

                Noi dung gom:
                top 10 token, top 10 trending, top 10 gainers, top 10 losers.
                """;
    }

    private String formatMarketList(List<MarketCrypto> cryptos) {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < cryptos.size(); i++) {
            MarketCrypto crypto = cryptos.get(i);
            message.append("%d. %s (%s): %s%% | Vol %s\n".formatted(
                    i + 1,
                    crypto.symbol(),
                    crypto.name(),
                    formatSignedPercent(crypto.changePercent24h()),
                    formatCompactMoney(crypto.totalVolumeUsd())
            ));
        }

        return message.toString().stripTrailing();
    }

    private String formatTrendingList(List<TrendingCrypto> cryptos) {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < cryptos.size(); i++) {
            TrendingCrypto crypto = cryptos.get(i);
            message.append("%d. %s (%s): %s%% | Vol %s\n".formatted(
                    i + 1,
                    crypto.symbol(),
                    crypto.name(),
                    formatSignedPercent(crypto.changePercent24h()),
                    formatCompactMoney(crypto.totalVolumeUsd())
            ));
        }

        return message.toString().stripTrailing();
    }

    private Optional<Long> parseChatId(String rawChatId) {
        try {
            return Optional.of(Long.parseLong(rawChatId));
        } catch (NumberFormatException exception) {
            redisTemplate.opsForSet().remove(DAILY_SUBSCRIBERS_KEY, rawChatId);
            return Optional.empty();
        }
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
