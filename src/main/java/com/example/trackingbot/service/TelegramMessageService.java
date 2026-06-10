package com.example.trackingbot.service;

import com.example.trackingbot.config.TelegramBotProperties;
import com.example.trackingbot.dto.SendMessageRequest;
import com.example.trackingbot.dto.SendPhotoRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramMessageService {

    private final RestClient restClient;
    private final TelegramBotProperties properties;

    public TelegramMessageService(RestClient.Builder restClientBuilder, TelegramBotProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.telegram.org/bot" + properties.token())
                .build();
        this.properties = properties;
    }

    public void sendTextMessage(Long chatId, String text) {
        restClient.post()
                .uri("/sendMessage")
                .body(new SendMessageRequest(chatId, text))
                .retrieve()
                .toBodilessEntity();
    }

    public void sendPhoto(Long chatId, String photoUrl, String caption) {
        restClient.post()
                .uri("/sendPhoto")
                .body(new SendPhotoRequest(chatId, photoUrl, caption))
                .retrieve()
                .toBodilessEntity();
    }

    public boolean isValidSecret(String secretHeader) {
        String configuredSecret = properties.webhookSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            return true;
        }

        return configuredSecret.equals(secretHeader);
    }
}
