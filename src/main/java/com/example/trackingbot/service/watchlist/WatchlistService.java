package com.example.trackingbot.service.watchlist;

import com.example.trackingbot.model.UserWatchlist;
import com.example.trackingbot.entity.WatchlistItemEntity;
import com.example.trackingbot.dto.response.CryptoPrice;
import com.example.trackingbot.repository.WatchlistItemRepository;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.daily.DailyMarketSummaryService;
import com.example.trackingbot.service.telegram.TelegramUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final CryptoPriceService cryptoPriceService;
    private final TelegramUserService telegramUserService;
    private final WatchlistItemRepository watchlistItemRepository;
    private final DailyMarketSummaryService dailyMarketSummaryService;

    @Transactional
    public String addToWatchlist(Long chatId, String rawSymbol) {
        String symbol = cryptoPriceService.normalizeSymbol(rawSymbol);
        Optional<String> coinId = cryptoPriceService.findCoinId(symbol);

        if (coinId.isEmpty()) {
            return unsupportedSymbolMessage();
        }

        if (watchlistItemRepository.existsByUserChatIdAndSymbol(chatId, symbol)) {
            return symbol + " da co trong watchlist cua ban roi.";
        }

        watchlistItemRepository.save(new WatchlistItemEntity(telegramUserService.getOrCreateUser(chatId), symbol));
        return "Da them " + symbol + " vao watchlist.";
    }

    @Transactional
    public String removeFromWatchlist(Long chatId, String rawSymbol) {
        String symbol = cryptoPriceService.normalizeSymbol(rawSymbol);
        if (symbol.isBlank()) {
            return getHelpMessage();
        }

        long removedCount = watchlistItemRepository.deleteByUserChatIdAndSymbol(chatId, symbol);
        if (removedCount == 0) {
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
        dailyMarketSummaryService.setWatchUpdatesEnabled(chatId, true);
        return """
                Da bat tu dong cap nhat watchlist moi 5 phut.

                Bot se gui gia cac ma trong watchlist cua ban.
                """;
    }

    public String disableWatchUpdates(Long chatId) {
        dailyMarketSummaryService.setWatchUpdatesEnabled(chatId, false);
        return """
                Da tat tu dong cap nhat watchlist.

                Ban van co the xem bat cu luc nao bang:
                /mywatchlist
                """;
    }

    public boolean isWatchUpdatesEnabled(Long chatId) {
        return dailyMarketSummaryService.isWatchUpdatesEnabled(chatId);
    }

    @Transactional(readOnly = true)
    public List<UserWatchlist> getAllWatchlists() {
        Map<Long, List<String>> groupedSymbols = new LinkedHashMap<>();
        for (WatchlistItemEntity item : watchlistItemRepository.findAllByOrderByUserChatIdAscSymbolAsc()) {
            groupedSymbols.computeIfAbsent(item.getUser().getChatId(), ignored -> new ArrayList<>())
                    .add(item.getSymbol());
        }

        return groupedSymbols.entrySet()
                .stream()
                .map(entry -> new UserWatchlist(entry.getKey(), entry.getValue()))
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

    @Transactional(readOnly = true)
    private List<String> getSymbols(Long chatId) {
        return watchlistItemRepository.findByUserChatIdOrderBySymbolAsc(chatId)
                .stream()
                .map(WatchlistItemEntity::getSymbol)
                .toList();
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
