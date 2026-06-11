package com.example.trackingbot.service.watchlist;

import com.example.trackingbot.dto.response.CryptoPrice;
import com.example.trackingbot.entity.TelegramUser;
import com.example.trackingbot.entity.WatchlistItemEntity;
import com.example.trackingbot.repository.WatchlistItemRepository;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.daily.DailyMarketSummaryService;
import com.example.trackingbot.service.telegram.TelegramUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private CryptoPriceService cryptoPriceService;

    @Mock
    private TelegramUserService telegramUserService;

    @Mock
    private WatchlistItemRepository watchlistItemRepository;

    @Mock
    private DailyMarketSummaryService dailyMarketSummaryService;

    @InjectMocks
    private WatchlistService watchlistService;

    @Test
    void addToWatchlist_shouldSaveNewSupportedSymbol() {
        Long chatId = 123L;
        TelegramUser user = new TelegramUser(chatId);
        when(cryptoPriceService.normalizeSymbol("btc")).thenReturn("BTC");
        when(cryptoPriceService.findCoinId("BTC")).thenReturn(Optional.of("bitcoin"));
        when(watchlistItemRepository.existsByUserChatIdAndSymbol(chatId, "BTC")).thenReturn(false);
        when(telegramUserService.getOrCreateUser(chatId)).thenReturn(user);
        when(watchlistItemRepository.save(any(WatchlistItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = watchlistService.addToWatchlist(chatId, "btc");

        ArgumentCaptor<WatchlistItemEntity> captor = ArgumentCaptor.forClass(WatchlistItemEntity.class);
        verify(watchlistItemRepository).save(captor.capture());

        assertThat(result).isEqualTo("Da them BTC vao watchlist.");
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getSymbol()).isEqualTo("BTC");
    }

    @Test
    void addToWatchlist_shouldNotSaveDuplicateSymbol() {
        Long chatId = 123L;
        when(cryptoPriceService.normalizeSymbol("BTC")).thenReturn("BTC");
        when(cryptoPriceService.findCoinId("BTC")).thenReturn(Optional.of("bitcoin"));
        when(watchlistItemRepository.existsByUserChatIdAndSymbol(chatId, "BTC")).thenReturn(true);

        String result = watchlistService.addToWatchlist(chatId, "BTC");

        assertThat(result).isEqualTo("BTC da co trong watchlist cua ban roi.");
        verify(watchlistItemRepository, never()).save(any());
    }

    @Test
    void getWatchlistMessage_shouldIncludeCurrentPriceAndDailyChange() {
        Long chatId = 123L;
        TelegramUser user = new TelegramUser(chatId);
        when(watchlistItemRepository.findByUserChatIdOrderBySymbolAsc(chatId)).thenReturn(List.of(
                new WatchlistItemEntity(user, "BTC"),
                new WatchlistItemEntity(user, "ETH")
        ));
        when(cryptoPriceService.getCurrentPrice("BTC")).thenReturn(price("BTC", "70000", "1.25"));
        when(cryptoPriceService.getCurrentPrice("ETH")).thenReturn(price("ETH", "3800", "-0.4"));

        String result = watchlistService.getWatchlistMessage(chatId);

        assertThat(result).contains("Watchlist cua ban:");
        assertThat(result).contains("BTC: 70,000.00 USD | 24h: +1.25%");
        assertThat(result).contains("ETH: 3,800.00 USD | 24h: -0.40%");
    }

    private CryptoPrice price(String symbol, String price, String changePercent24h) {
        return new CryptoPrice(
                symbol,
                new BigDecimal(price),
                new BigDecimal(changePercent24h),
                new BigDecimal("1000000000"),
                new BigDecimal(price),
                new BigDecimal(price),
                Instant.now()
        );
    }
}
