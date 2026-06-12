package com.example.trackingbot.service.notification;

import com.example.trackingbot.config.RabbitMqConfig;
import com.example.trackingbot.model.TelegramNotification;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private final TelegramMessageService telegramMessageService;

    @RabbitListener(queues = RabbitMqConfig.TELEGRAM_NOTIFICATION_QUEUE)
    public void consume(TelegramNotification telegramNotification) {
        telegramMessageService.sendTextMessage
                (
                        telegramNotification.chatId(),
                        telegramNotification.text()
                );
    }
}
