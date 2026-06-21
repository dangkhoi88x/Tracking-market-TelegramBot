package com.example.trackingbot.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    // config notifcation flow async with rabbiqmq
    public static final String TELEGRAM_NOTIFICATION_EXCHANGE = "telegram.notification.exchange";
    public static final String TELEGRAM_NOTIFICATION_QUEUE = "telegram.notification.queue";
    public static final String TELEGRAM_NOTIFICATION_ROUTING_KEY = "telegram.notification";

    public static final String TELEGRAM_ALERT_QUEUE = "telegram.notification.alert.queue";
    public static final String TELEGRAM_WATCHLIST_QUEUE = "telegram.notification.watchlist.queue";
    public static final String TELEGRAM_DAILY_QUEUE = "telegram.notification.daily.queue";

    public static final String TELEGRAM_ALERT_ROUTING_KEY = "telegram.notification.alert";
    public static final String TELEGRAM_WATCHLIST_ROUTING_KEY = "telegram.notification.watchlist";
    public static final String TELEGRAM_DAILY_ROUTING_KEY = "telegram.notification.daily";

    public static final String TELEGRAM_NOTIFICATION_DLX = "telegram.notification.dlx";
    public static final String TELEGRAM_NOTIFICATION_DLQ = "telegram.notification.dlq";
    public static final String TELEGRAM_NOTIFICATION_DLQ_ROUTING_KEY = "telegram.notification.dlq";

    public static final String TELEGRAM_RETRY_10S_EXCHANGE = "telegram.notification.retry.10s.exchange";
    public static final String TELEGRAM_RETRY_30S_EXCHANGE = "telegram.notification.retry.30s.exchange";
    public static final String TELEGRAM_RETRY_60S_EXCHANGE = "telegram.notification.retry.60s.exchange";

    public static final String TELEGRAM_RETRY_10S_QUEUE = "telegram.notification.retry.10s.queue";
    public static final String TELEGRAM_RETRY_30S_QUEUE = "telegram.notification.retry.30s.queue";
    public static final String TELEGRAM_RETRY_60S_QUEUE = "telegram.notification.retry.60s.queue";

    public static final String TELEGRAM_RETRY_ATTEMPT_HEADER = "telegram-notification-retry-attempt";
    public static final String TELEGRAM_ORIGINAL_ROUTING_KEY_HEADER = "telegram-notification-original-routing-key";
    public static final String TELEGRAM_RETRY_DELAY_HEADER = "telegram-notification-retry-delay-ms";

    @Bean
    public DirectExchange telegramNotificationExchange() {
        return new DirectExchange(TELEGRAM_NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange telegramNotificationDeadLetterExchange() {
        return new DirectExchange(TELEGRAM_NOTIFICATION_DLX);
    }

    @Bean
    public DirectExchange telegramRetry10sExchange() {
        return new DirectExchange(TELEGRAM_RETRY_10S_EXCHANGE);
    }

    @Bean
    public DirectExchange telegramRetry30sExchange() {
        return new DirectExchange(TELEGRAM_RETRY_30S_EXCHANGE);
    }

    @Bean
    public DirectExchange telegramRetry60sExchange() {
        return new DirectExchange(TELEGRAM_RETRY_60S_EXCHANGE);
    }

    @Bean
    public Queue telegramNotificationQueue() {
        return durableNotificationQueue(TELEGRAM_NOTIFICATION_QUEUE);
    }

    @Bean
    public Queue telegramAlertQueue() {
        return durableNotificationQueue(TELEGRAM_ALERT_QUEUE);
    }

    @Bean
    public Queue telegramWatchlistQueue() {
        return durableNotificationQueue(TELEGRAM_WATCHLIST_QUEUE);
    }

    @Bean
    public Queue telegramDailyQueue() {
        return durableNotificationQueue(TELEGRAM_DAILY_QUEUE);
    }

    @Bean
    public Queue telegramNotificationDeadLetterQueue() {
        return QueueBuilder
                .durable(TELEGRAM_NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue telegramRetry10sQueue() {
        return retryQueue(TELEGRAM_RETRY_10S_QUEUE, 10_000);
    }

    @Bean
    public Queue telegramRetry30sQueue() {
        return retryQueue(TELEGRAM_RETRY_30S_QUEUE, 30_000);
    }

    @Bean
    public Queue telegramRetry60sQueue() {
        return retryQueue(TELEGRAM_RETRY_60S_QUEUE, 60_000);
    }

    @Bean
    public Binding telegramNotificationBinding() {
        return BindingBuilder
                .bind(telegramNotificationQueue())
                .to(telegramNotificationExchange())
                .with(TELEGRAM_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding telegramAlertBinding() {
        return BindingBuilder
                .bind(telegramAlertQueue())
                .to(telegramNotificationExchange())
                .with(TELEGRAM_ALERT_ROUTING_KEY);
    }

    @Bean
    public Binding telegramWatchlistBinding() {
        return BindingBuilder
                .bind(telegramWatchlistQueue())
                .to(telegramNotificationExchange())
                .with(TELEGRAM_WATCHLIST_ROUTING_KEY);
    }

    @Bean
    public Binding telegramDailyBinding() {
        return BindingBuilder
                .bind(telegramDailyQueue())
                .to(telegramNotificationExchange())
                .with(TELEGRAM_DAILY_ROUTING_KEY);
    }

    @Bean
    public Binding telegramNotificationDeadLetterBinding() {
        return BindingBuilder
                .bind(telegramNotificationDeadLetterQueue())
                .to(telegramNotificationDeadLetterExchange())
                .with(TELEGRAM_NOTIFICATION_DLQ_ROUTING_KEY);
    }

    @Bean
    public Declarables telegramRetry10sBindings() {
        return retryBindings(telegramRetry10sQueue(), telegramRetry10sExchange());
    }

    @Bean
    public Declarables telegramRetry30sBindings() {
        return retryBindings(telegramRetry30sQueue(), telegramRetry30sExchange());
    }

    @Bean
    public Declarables telegramRetry60sBindings() {
        return retryBindings(telegramRetry60sQueue(), telegramRetry60sExchange());
    }

    @Bean
    public MessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    private Queue durableNotificationQueue(String queueName) {
        return QueueBuilder
                .durable(queueName)
                .deadLetterExchange(TELEGRAM_NOTIFICATION_DLX)
                .deadLetterRoutingKey(TELEGRAM_NOTIFICATION_DLQ_ROUTING_KEY)
                .build();
    }

    private Queue retryQueue(String queueName, int ttlMs) {
        return QueueBuilder
                .durable(queueName)
                .ttl(ttlMs)
                .deadLetterExchange(TELEGRAM_NOTIFICATION_EXCHANGE)
                .build();
    }

    private Declarables retryBindings(Queue retryQueue, DirectExchange retryExchange) {
        return new Declarables(
                BindingBuilder.bind(retryQueue).to(retryExchange).with(TELEGRAM_NOTIFICATION_ROUTING_KEY),
                BindingBuilder.bind(retryQueue).to(retryExchange).with(TELEGRAM_ALERT_ROUTING_KEY),
                BindingBuilder.bind(retryQueue).to(retryExchange).with(TELEGRAM_WATCHLIST_ROUTING_KEY),
                BindingBuilder.bind(retryQueue).to(retryExchange).with(TELEGRAM_DAILY_ROUTING_KEY)
        );
    }
}
