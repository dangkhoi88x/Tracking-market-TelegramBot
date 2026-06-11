package com.example.trackingbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.trackingbot.config.IdeaChartProperties;
import com.example.trackingbot.config.OpenAiProperties;
import com.example.trackingbot.config.TelegramBotProperties;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({
        TelegramBotProperties.class,
        IdeaChartProperties.class,
        OpenAiProperties.class
})
public class TrackingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackingBotApplication.class, args);
    }

}
