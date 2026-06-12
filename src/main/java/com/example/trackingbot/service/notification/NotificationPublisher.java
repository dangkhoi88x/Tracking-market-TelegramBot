package com.example.trackingbot.service.notification;

import com.example.trackingbot.config.RabbitMqConfig;
import com.example.trackingbot.model.TelegramNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {

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
    }
}
