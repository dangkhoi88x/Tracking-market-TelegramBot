package com.example.trackingbot.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramBotProperties(
        //gom config liên quan Telegram bot
        @NotBlank String token,
        String webhookSecret,
        String adminChatId
) {
}
