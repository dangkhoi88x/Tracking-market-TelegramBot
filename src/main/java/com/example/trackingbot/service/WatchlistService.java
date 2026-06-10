package com.example.trackingbot.service;

import com.example.trackingbot.dto.CryptoPrice;
import com.example.trackingbot.dto.UserWatchlist;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class WatchlistService {

    private static final String WATCHLIST_KEY_PREFIX = "watchlist:";
    private static final String WATCH_UPDATES_KEY_PREFIX = "watch_updates:";
    private static final String WATCH_UPDATES_ON = "on";
    private static final String WATCH_UPDATES_OFF = "off";

    private final StringRedisTemplate redisTemplate;
    private final CryptoPriceService cryptoPriceService;

    public WatchlistService(StringRedisTemplate redisTemplate, CryptoPriceService cryptoPriceService) {
        this.redisTemplate = redisTemplate;
        this.cryptoPriceService = cryptoPriceService;
    }

    public String addToWatchlist(Long chatId, String rawSymbol) {
        String symbol = cryptoPriceService.normalizeSymbol(rawSymbol);
        Optional<String> coinId = cryptoPriceService.findCoinId(symbol);

        if (coinId.isEmpty()) {
            return unsupportedSymbolMessage();
        }

        Long addedCount = redisTemplate.opsForSet().add(watchlistKey(chatId), symbol);
        if (addedCount != null && addedCount == 0) {
            return symbol + " da co trong watchlist cua ban roi.";
        }

        return "Da them " + symbol + " vao watchlist.";
    }

    public String removeFromWatchlist(Long chatId, String rawSymbol) {
        String symbol = cryptoPriceService.normalizeSymbol(rawSymbol);
        if (symbol.isBlank()) {
            return getHelpMessage();
        }

        Long removedCount = redisTemplate.opsForSet().remove(watchlistKey(chatId), symbol);
        if (removedCount == null || removedCount == 0) {
            return symbol + " khong co trong watchlist cua ban.";
        }

        return "Da xoa " + symbol + " khoi watchlist.";
    }

    public String getWatchlistMessage(Long chatId) {
        List<String> symbols = getSymbols(chatId);

        if (symbols.isEmpty()) {
            return """
                    Watchlist cua ban dang trong.

                    Them ma dau tien:
                    /watch BTC
                    """;
        }

        return buildWatchlistPriceMessage("Watchlist cua ban:", symbols);
    }

    public String buildWatchlistUpdateMessage(UserWatchlist watchlist) {
        return buildWatchlistPriceMessage("Cap nhat watchlist moi 5 phut:", watchlist.symbols());
    }

    public String enableWatchUpdates(Long chatId) {
        redisTemplate.opsForValue().set(watchUpdatesKey(chatId), WATCH_UPDATES_ON);
        return """
                Da bat tu dong cap nhat watchlist moi 5 phut.

                Bot se gui gia cac ma trong watchlist cua ban.
                """;
    }

    public String disableWatchUpdates(Long chatId) {
        redisTemplate.opsForValue().set(watchUpdatesKey(chatId), WATCH_UPDATES_OFF);
        return """
                Da tat tu dong cap nhat watchlist.

                Ban van co the xem bat cu luc nao bang:
                /mywatchlist
                """;
    }

    public boolean isWatchUpdatesEnabled(Long chatId) {
        String setting = redisTemplate.opsForValue().get(watchUpdatesKey(chatId));
        return setting == null || WATCH_UPDATES_ON.equalsIgnoreCase(setting);
    }

    public List<UserWatchlist> getAllWatchlists() {
        Set<String> watchlistKeys = redisTemplate.keys(WATCHLIST_KEY_PREFIX + "*");
        return Optional.ofNullable(watchlistKeys)
                .orElse(Collections.emptySet())
                .stream()
                .map(this::toUserWatchlist)
                .flatMap(Optional::stream)
                .filter(watchlist -> !watchlist.symbols().isEmpty())
                .toList();
    }

    public String getHelpMessage() {
        return """
                Cach dung:
                /watch BTC - them vao watchlist
                /unwatch BTC - xoa khoi watchlist
                /mywatchlist - xem watchlist
                /watch_updates_on - bat gui tu dong moi 5 phut
                /watch_updates_off - tat gui tu dong

                Ma crypto dang ho tro:
                %s
                """.formatted(cryptoPriceService.getSupportedSymbolsText());
    }

    private String unsupportedSymbolMessage() {
        return """
                Minh chua ho tro ma crypto nay.

                Ma crypto dang ho tro:
                %s
                """.formatted(cryptoPriceService.getSupportedSymbolsText());
    }

    private String watchlistKey(Long chatId) {
        return WATCHLIST_KEY_PREFIX + chatId;
    }

    private String watchUpdatesKey(Long chatId) {
        return WATCH_UPDATES_KEY_PREFIX + chatId;
    }

    private List<String> getSymbols(Long chatId) {
        Set<String> watchlistMembers = redisTemplate.opsForSet().members(watchlistKey(chatId));
        return Optional.ofNullable(watchlistMembers)
                .orElse(Collections.emptySet())
                .stream()
                .sorted()
                .toList();
    }

    private Optional<UserWatchlist> toUserWatchlist(String watchlistKey) {
        String rawChatId = watchlistKey.substring(WATCHLIST_KEY_PREFIX.length());
        try {
            return Optional.of(new UserWatchlist(
                    Long.parseLong(rawChatId),
                    getSymbols(Long.parseLong(rawChatId))
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String buildWatchlistPriceMessage(String title, List<String> symbols) {
        StringBuilder message = new StringBuilder(title).append("\n\n");

        for (String symbol : symbols) {
            try {
                CryptoPrice price = cryptoPriceService.getCurrentPrice(symbol);
                message.append("%s: %s USD | 24h: %s%%\n".formatted(
                        symbol,
                        formatMoney(price.priceUsd()),
                        formatSignedPercent(price.changePercent24h())
                ));
            } catch (Exception exception) {
                message.append(symbol).append(": tam thoi khong lay duoc gia\n");
            }
        }

        return message.toString();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "N/A";
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
}
