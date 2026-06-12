package com.example.trackingbot.service.notification;

import com.example.trackingbot.config.RabbitMqConfig;
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

    public void publishTelegramNotification(Long chatId, String type, String text) {
        TelegramNotification notification = new TelegramNotification(
                UUID.randomUUID().toString(),
                chatId,
                type,
                text,
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.TELEGRAM_NOTIFICATION_EXCHANGE,
                RabbitMqConfig.TELEGRAM_NOTIFICATION_ROUTING_KEY,
                notification
        );

        log.info(
                "Published Telegram notification id={} type={} chatId={}",
                notification.id(),
                notification.type(),
                notification.chatId()
        );
    }
}