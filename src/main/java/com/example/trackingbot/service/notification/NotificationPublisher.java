package com.example.trackingbot.service.notification;

import com.example.trackingbot.config.RabbitMqConfig;
import com.example.trackingbot.model.NotificationType;
import com.example.trackingbot.model.TelegramNotification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publishTelegramNotification(Long chatId, NotificationType type, String text) {
        TelegramNotification notification = new TelegramNotification(
                UUID.randomUUID().toString(),
                chatId,
                type,
                text,
                Instant.now()
        );
        String routingKey = routingKeyFor(type);

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.TELEGRAM_NOTIFICATION_EXCHANGE,
                routingKey,
                notification
        );

        log.info(
                "Published Telegram notification id={} type={} chatId={} routingKey={}",
                notification.id(),
                notification.type(),
                notification.chatId(),
                routingKey
        );
    }

    private String routingKeyFor(NotificationType type) {
        return switch (type) {
            case ALERT_TRIGGERED -> RabbitMqConfig.TELEGRAM_ALERT_ROUTING_KEY;
            case WATCHLIST_UPDATE -> RabbitMqConfig.TELEGRAM_WATCHLIST_ROUTING_KEY;
            case DAILY_SUMMARY -> RabbitMqConfig.TELEGRAM_DAILY_ROUTING_KEY;
            case GENERAL -> RabbitMqConfig.TELEGRAM_NOTIFICATION_ROUTING_KEY;
        };
    }
}
