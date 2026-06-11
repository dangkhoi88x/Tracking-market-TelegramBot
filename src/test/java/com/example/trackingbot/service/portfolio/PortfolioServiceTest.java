package com.example.trackingbot.service.portfolio;

import com.example.trackingbot.dto.response.CryptoPrice;
import com.example.trackingbot.entity.PortfolioPositionEntity;
import com.example.trackingbot.entity.TelegramUser;
import com.example.trackingbot.repository.PortfolioPositionRepository;
import com.example.trackingbot.service.crypto.CryptoPriceService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private CryptoPriceService cryptoPriceService;

    @Mock
    private TelegramUserService telegramUserService;

    @Mock
    private PortfolioPositionRepository portfolioPositionRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void addBuy_shouldSaveBuyPositionWithAmountAndEntryPrice() {
        Long chatId = 123L;
        TelegramUser user = new TelegramUser(chatId);
        when(cryptoPriceService.normalizeSymbol("BTC")).thenReturn("BTC");
        when(cryptoPriceService.findCoinId("BTC")).thenReturn(Optional.of("bitcoin"));
        when(telegramUserService.getOrCreateUser(chatId)).thenReturn(user);
        when(portfolioPositionRepository.save(any(PortfolioPositionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = portfolioService.addBuy(chatId, "BTC 0.1 65000");

        ArgumentCaptor<PortfolioPositionEntity> captor = ArgumentCaptor.forClass(PortfolioPositionEntity.class);
        verify(portfolioPositionRepository).save(captor.capture());
        PortfolioPositionEntity savedPosition = captor.getValue();

        assertThat(result).contains("Da luu BUY position", "BUY BTC", "65,000.00 USD");
        assertThat(savedPosition.getUser()).isSameAs(user);
        assertThat(savedPosition.getSide()).isEqualTo("BUY");
        assertThat(savedPosition.getSymbol()).isEqualTo("BTC");
        assertThat(savedPosition.getAmount()).isEqualByComparingTo(new BigDecimal("0.1"));
        assertThat(savedPosition.getEntryPrice()).isEqualByComparingTo(new BigDecimal("65000"));
    }

    @Test
    void getPortfolioMessage_shouldCalculateBuyProfitLoss() {
        Long chatId = 123L;
        TelegramUser user = new TelegramUser(chatId);
        PortfolioPositionEntity position = new PortfolioPositionEntity(
                "position-1",
                user,
                "BUY",
                "BTC",
                new BigDecimal("0.1"),
                new BigDecimal("65000")
        );

        when(portfolioPositionRepository.findByUserChatIdOrderByCreatedAtDesc(chatId)).thenReturn(List.of(position));
        when(cryptoPriceService.getCurrentPrice("BTC")).thenReturn(new CryptoPrice(
                "BTC",
                new BigDecimal("70000"),
                new BigDecimal("1.5"),
                new BigDecimal("1000000000"),
                new BigDecimal("71000"),
                new BigDecimal("64000"),
                Instant.now()
        ));

        String result = portfolioService.getPortfolioMessage(chatId);

        assertThat(result).contains("BTC BUY");
        assertThat(result).contains("Amount: 0.1 coin");
        assertThat(result).contains("Gia mua: 65,000.00 USD");
        assertThat(result).contains("Gia hien tai: 70,000.00 USD");
        assertThat(result).contains("P/L: +500.00 USD");
    }
}
