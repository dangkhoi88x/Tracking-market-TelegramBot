package com.example.trackingbot.service.notification;

import com.example.trackingbot.config.RabbitMqConfig;
import com.example.trackingbot.model.TelegramNotification;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private final TelegramMessageService telegramMessageService;
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    @RabbitListener(queues = RabbitMqConfig.TELEGRAM_NOTIFICATION_QUEUE)
    public void consume(TelegramNotification telegramNotification) {
        log.info(
                "Consuming Telegram notification id={} type={} chatId={}",
                telegramNotification.id(),
                telegramNotification.type(),
                telegramNotification.chatId()
        );
        telegramMessageService.sendTextMessage
                (
                        telegramNotification.chatId(),
                        telegramNotification.text()
                );
        log.info(
                "Consumed Telegram notification id={} type={} chatId={}",
                telegramNotification.id(),
                telegramNotification.type(),
                telegramNotification.chatId()
        );
    }
}
