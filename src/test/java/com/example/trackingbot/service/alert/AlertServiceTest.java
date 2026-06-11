package com.example.trackingbot.service.alert;

import com.example.trackingbot.entity.PriceAlertEntity;
import com.example.trackingbot.entity.TelegramUser;
import com.example.trackingbot.repository.PriceAlertRepository;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.telegram.TelegramUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private CryptoPriceService cryptoPriceService;

    @Mock
    private TelegramUserService telegramUserService;

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @InjectMocks
    private AlertService alertService;

    @Test
    void createAlert_shouldSaveActiveAlertWhenCommandIsValid() {
        Long chatId = 123L;
        TelegramUser user = new TelegramUser(chatId);
        when(cryptoPriceService.normalizeSymbol("btc")).thenReturn("BTC");
        when(cryptoPriceService.findCoinId("BTC")).thenReturn(Optional.of("bitcoin"));
        when(telegramUserService.getOrCreateUser(chatId)).thenReturn(user);
        when(priceAlertRepository.save(any(PriceAlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = alertService.createAlert(chatId, "btc > 70000");

        ArgumentCaptor<PriceAlertEntity> captor = ArgumentCaptor.forClass(PriceAlertEntity.class);
        verify(priceAlertRepository).save(captor.capture());
        PriceAlertEntity savedAlert = captor.getValue();

        assertThat(result).contains("Da tao alert", "BTC > 70000");
        assertThat(savedAlert.getUser()).isSameAs(user);
        assertThat(savedAlert.getSymbol()).isEqualTo("BTC");
        assertThat(savedAlert.getOperator()).isEqualTo(">");
        assertThat(savedAlert.getTargetPrice()).isEqualByComparingTo(new BigDecimal("70000"));
        assertThat(savedAlert.isActive()).isTrue();
    }

    @Test
    void createAlert_shouldRejectUnsupportedOperator() {
        assertThatThrownBy(() -> alertService.createAlert(123L, "BTC == 70000"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(priceAlertRepository, never()).save(any());
    }
}
