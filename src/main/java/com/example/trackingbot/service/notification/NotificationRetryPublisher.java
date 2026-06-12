package com.example.trackingbot.service.notification;

import com.example.trackingbot.config.RabbitMqConfig;
import com.example.trackingbot.model.TelegramNotification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationRetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishRetry(TelegramNotification notification, String originalRoutingKey, int retryAttempt) {
        String retryExchange = retryExchangeFor(retryAttempt);
        int delayMs = delayMsFor(retryAttempt);

        rabbitTemplate.convertAndSend(
                retryExchange,
                originalRoutingKey,
                notification,
                message -> {
                    message.getMessageProperties().setHeader(
                            RabbitMqConfig.TELEGRAM_RETRY_ATTEMPT_HEADER,
                            retryAttempt
                    );
                    message.getMessageProperties().setHeader(
                            RabbitMqConfig.TELEGRAM_ORIGINAL_ROUTING_KEY_HEADER,
                            originalRoutingKey
                    );
                    message.getMessageProperties().setHeader(
                            RabbitMqConfig.TELEGRAM_RETRY_DELAY_HEADER,
                            delayMs
                    );
                    return message;
                }
        );

        log.warn(
                "Scheduled Telegram notification retry id={} type={} chatId={} attempt={} delayMs={} routingKey={}",
                notification.id(),
                notification.type(),
                notification.chatId(),
                retryAttempt,
                delayMs,
                originalRoutingKey
        );
    }

    private String retryExchangeFor(int retryAttempt) {
        return switch (retryAttempt) {
            case 1 -> RabbitMqConfig.TELEGRAM_RETRY_10S_EXCHANGE;
            case 2 -> RabbitMqConfig.TELEGRAM_RETRY_30S_EXCHANGE;
            case 3 -> RabbitMqConfig.TELEGRAM_RETRY_60S_EXCHANGE;
            default -> throw new IllegalArgumentException("Unsupported retry attempt: " + retryAttempt);
        };
    }

    private int delayMsFor(int retryAttempt) {
        return switch (retryAttempt) {
            case 1 -> 10_000;
            case 2 -> 30_000;
            case 3 -> 60_000;
            default -> throw new IllegalArgumentException("Unsupported retry attempt: " + retryAttempt);
        };
    }
}
