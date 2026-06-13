package com.example.trackingbot.service.notification;

import com.example.trackingbot.config.RabbitMqConfig;
import com.example.trackingbot.model.TelegramNotification;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final int MAX_RETRY_ATTEMPT = 3;
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final TelegramMessageService telegramMessageService;
    private final NotificationRetryPublisher notificationRetryPublisher;
    private final NotificationHistoryService notificationHistoryService;

    @RabbitListener(queues = {
            RabbitMqConfig.TELEGRAM_NOTIFICATION_QUEUE,
            RabbitMqConfig.TELEGRAM_ALERT_QUEUE,
            RabbitMqConfig.TELEGRAM_WATCHLIST_QUEUE,
            RabbitMqConfig.TELEGRAM_DAILY_QUEUE
    })
    public void consume(
            TelegramNotification telegramNotification,
            @Header(AmqpHeaders.CONSUMER_QUEUE) String queueName,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
            @Header(value = RabbitMqConfig.TELEGRAM_RETRY_ATTEMPT_HEADER, required = false) Integer retryAttempt
    ) {
        log.info(
                "Consuming Telegram notification id={} type={} chatId={} queue={} routingKey={} retryAttempt={}",
                telegramNotification.id(),
                telegramNotification.type(),
                telegramNotification.chatId(),
                queueName,
                routingKey,
                currentRetryAttempt(retryAttempt)
        );

        try {
            telegramMessageService.sendTextMessage(
                    telegramNotification.chatId(),
                    telegramNotification.text()
            );
            notificationHistoryService.recordSent(telegramNotification);
        } catch (Exception exception) {
            handleFailedNotification(telegramNotification, queueName, routingKey, retryAttempt, exception);
            return;
        }

        log.info(
                "Consumed Telegram notification id={} type={} chatId={} queue={}",
                telegramNotification.id(),
                telegramNotification.type(),
                telegramNotification.chatId(),
                queueName
        );
    }

    private void handleFailedNotification(
            TelegramNotification telegramNotification,
            String queueName,
            String routingKey,
            Integer retryAttempt,
            Exception exception
    ) {
        int currentAttempt = currentRetryAttempt(retryAttempt);

        if (currentAttempt >= MAX_RETRY_ATTEMPT) {
            log.error(
                    "Telegram notification failed after max retries id={} type={} chatId={} queue={} routingKey={} retryAttempt={}",
                    telegramNotification.id(),
                    telegramNotification.type(),
                    telegramNotification.chatId(),
                    queueName,
                    routingKey,
                    currentAttempt,
                    exception
            );
            throw new AmqpRejectAndDontRequeueException(
                    "Telegram notification failed after max retries",
                    exception
            );
        }

        int nextAttempt = currentAttempt + 1;
        notificationRetryPublisher.publishRetry(telegramNotification, routingKey, nextAttempt);

        log.warn(
                "Telegram notification failed and was moved to retry id={} type={} chatId={} queue={} routingKey={} nextAttempt={}",
                telegramNotification.id(),
                telegramNotification.type(),
                telegramNotification.chatId(),
                queueName,
                routingKey,
                nextAttempt,
                exception
        );
    }

    private int currentRetryAttempt(Integer retryAttempt) {
        return retryAttempt == null ? 0 : retryAttempt;
    }
}
