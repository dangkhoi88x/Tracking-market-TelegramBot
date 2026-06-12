package com.example.trackingbot.service.alert;

import com.example.trackingbot.model.CryptoAlert;
import com.example.trackingbot.dto.response.CryptoPrice;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AlertCheckerScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertCheckerScheduler.class);

    private final AlertService alertService;
    private final CryptoPriceService cryptoPriceService;
    private final NotificationPublisher notificationPublisher;

    @Scheduled(fixedRate = 60_000, initialDelay = 10_000)
    public void checkActiveAlerts() {
        for (CryptoAlert alert : alertService.getActiveAlerts()) {
            try {
                CryptoPrice currentPrice = cryptoPriceService.getCurrentPrice(alert.symbol());
                if (isTriggered(currentPrice.priceUsd(), alert.operator(), alert.targetPrice())) {
                    notificationPublisher.publishTelegramNotification(
                            alert.chatId(),
                            "ALERT_TRIGGERED",
                            buildTriggeredMessage(alert, currentPrice)
                    );
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
