package com.example.trackingbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.trackingbot.config.TelegramBotProperties;

@SpringBootApplication
@EnableConfigurationProperties(TelegramBotProperties.class)
public class TrackingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackingBotApplication.class, args);
    }

}
