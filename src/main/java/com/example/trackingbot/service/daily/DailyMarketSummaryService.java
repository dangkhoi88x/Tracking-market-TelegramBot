package com.example.trackingbot.service.daily;

import com.example.trackingbot.client.CoinGeckoClient;
import com.example.trackingbot.entity.DailySettingEntity;
import com.example.trackingbot.dto.response.MarketCrypto;
import com.example.trackingbot.dto.response.TrendingCrypto;
import com.example.trackingbot.repository.DailySettingRepository;
import com.example.trackingbot.service.telegram.TelegramUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyMarketSummaryService {

    private static final int TOP_LIMIT = 10;
    private static final int MOVER_UNIVERSE_LIMIT = 250;

    private final CoinGeckoClient coinGeckoClient;
    private final TelegramUserService telegramUserService;
    private final DailySettingRepository dailySettingRepository;

    @Transactional
    public String enableDailySummary(Long chatId) {
        DailySettingEntity setting = getOrCreateSetting(chatId);
        setting.setEnabled(true);
        return """
                Da bat Daily Market Summary.

                Moi sang bot se gui:
                top 10 token, top 10 trending, top 10 gainers, top 10 losers.
                """;
    }

    @Transactional
    public String disableDailySummary(Long chatId) {
        DailySettingEntity setting = getOrCreateSetting(chatId);
        setting.setEnabled(false);
        return "Da tat Daily Market Summary.";
    }

    @Transactional
    public void setWatchUpdatesEnabled(Long chatId, boolean enabled) {
        DailySettingEntity setting = getOrCreateSetting(chatId);
        setting.setWatchUpdatesEnabled(enabled);
    }

    @Transactional(readOnly = true)
    public boolean isWatchUpdatesEnabled(Long chatId) {
        return dailySettingRepository.findByUserChatId(chatId)
                .map(DailySettingEntity::isWatchUpdatesEnabled)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public List<Long> getSubscriberChatIds() {
        return dailySettingRepository.findByEnabledTrueOrderByUserChatIdAsc()
                .stream()
                .map(setting -> setting.getUser().getChatId())
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

    private DailySettingEntity getOrCreateSetting(Long chatId) {
        return dailySettingRepository.findByUserChatId(chatId)
                .orElseGet(() -> dailySettingRepository.save(
                        new DailySettingEntity(telegramUserService.getOrCreateUser(chatId), false)
                ));
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
