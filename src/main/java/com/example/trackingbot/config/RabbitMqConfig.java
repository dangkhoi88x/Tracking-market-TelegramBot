package com.example.trackingbot.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String TELEGRAM_NOTIFICATION_EXCHANGE = "telegram.notification.exchange";
    public static final String TELEGRAM_NOTIFICATION_QUEUE = "telegram.notification.queue";
    public static final String TELEGRAM_NOTIFICATION_ROUTING_KEY = "telegram.notification";

    @Bean
    public DirectExchange telegramNotificationExchange() {
        return new DirectExchange(TELEGRAM_NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue telegramNotificationQueue() {
        return QueueBuilder.durable(TELEGRAM_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding telegramNotificationBinding() {
        return BindingBuilder.bind(telegramNotificationQueue())
                .to(telegramNotificationExchange())
                .with(TELEGRAM_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
