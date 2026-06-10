package com.example.trackingbot.service;

import com.example.trackingbot.dto.CryptoAlert;
import com.example.trackingbot.dto.CryptoPrice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AlertCheckerScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertCheckerScheduler.class);

    private final AlertService alertService;
    private final CryptoPriceService cryptoPriceService;
    private final TelegramMessageService telegramMessageService;

    public AlertCheckerScheduler(
            AlertService alertService,
            CryptoPriceService cryptoPriceService,
            TelegramMessageService telegramMessageService
    ) {
        this.alertService = alertService;
        this.cryptoPriceService = cryptoPriceService;
        this.telegramMessageService = telegramMessageService;
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 10_000)
    public void checkActiveAlerts() {
        for (CryptoAlert alert : alertService.getActiveAlerts()) {
            try {
                CryptoPrice currentPrice = cryptoPriceService.getCurrentPrice(alert.symbol());
                if (isTriggered(currentPrice.priceUsd(), alert.operator(), alert.targetPrice())) {
                    telegramMessageService.sendTextMessage(alert.chatId(), buildTriggeredMessage(alert, currentPrice));
                    alertService.markTriggered(alert.id());
                }
            } catch (Exception exception) {
                log.warn("Failed to check alert {}", alert.id(), exception);
            }
        }
    }

    private boolean isTriggered(BigDecimal currentPrice, String operator, BigDecimal targetPrice) {
        int comparison = currentPrice.compareTo(targetPrice);
        return switch (operator) {
            case ">" -> comparison > 0;
            case "<" -> comparison < 0;
            case ">=" -> comparison >= 0;
            case "<=" -> comparison <= 0;
            default -> false;
        };
    }

    private String buildTriggeredMessage(CryptoAlert alert, CryptoPrice currentPrice) {
        return """
                Alert da kich hoat!

                %s hien tai: %,.2f USD
                Dieu kien: %s %s %s USD

                Alert nay da duoc tat de tranh spam.
                """.formatted(
                alert.symbol(),
                currentPrice.priceUsd(),
                alert.symbol(),
                alert.operator(),
                alert.targetPrice().toPlainString()
        );
    }
}
