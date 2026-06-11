package com.example.trackingbot.service.telegram;

import com.example.trackingbot.config.TelegramBotProperties;
import com.example.trackingbot.dto.request.AnswerCallbackQueryRequest;
import com.example.trackingbot.dto.request.InlineKeyboardMarkup;
import com.example.trackingbot.dto.request.SendMessageRequest;
import com.example.trackingbot.dto.request.SendMessageWithKeyboardRequest;
import com.example.trackingbot.dto.request.SendPhotoRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;

@Service
public class TelegramMessageService {

    private static final int TELEGRAM_TEXT_LIMIT = 3900;

    private final RestClient restClient;
    private final TelegramBotProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramMessageService(RestClient.Builder restClientBuilder, TelegramBotProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.telegram.org/bot" + properties.token())
                .build();
        this.properties = properties;
    }

    public void sendTextMessage(Long chatId, String text) {
        if (text != null && text.length() > TELEGRAM_TEXT_LIMIT) {
            for (String chunk : splitMessage(text)) {
                sendSingleTextMessage(chatId, chunk);
            }
            return;
        }

        sendSingleTextMessage(chatId, text);
    }

    private void sendSingleTextMessage(Long chatId, String text) {
        restClient.post()
                .uri("/sendMessage")
                .body(new SendMessageRequest(chatId, text))
                .retrieve()
                .toBodilessEntity();
    }

    public void sendTextMessage(Long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        restClient.post()
                .uri("/sendMessage")
                .body(new SendMessageWithKeyboardRequest(chatId, text, replyMarkup))
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

    public void sendPhotoFile(Long chatId, Path photoPath, String caption) {
        sendPhotoFile(chatId, photoPath, caption, null);
    }

    public void sendPhotoFile(Long chatId, Path photoPath, String caption, InlineKeyboardMarkup replyMarkup) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId.toString());
        body.add("photo", new FileSystemResource(photoPath));
        body.add("caption", caption);
        if (replyMarkup != null) {
            body.add("reply_markup", toJson(replyMarkup));
        }

        restClient.post()
                .uri("/sendPhoto")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize Telegram reply markup", exception);
        }
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        restClient.post()
                .uri("/answerCallbackQuery")
                .body(new AnswerCallbackQueryRequest(callbackQueryId, text))
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

    private java.util.List<String> splitMessage(String text) {
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        String remaining = text;
        while (remaining.length() > TELEGRAM_TEXT_LIMIT) {
            int splitIndex = remaining.lastIndexOf("\n\n", TELEGRAM_TEXT_LIMIT);
            if (splitIndex < TELEGRAM_TEXT_LIMIT / 2) {
                splitIndex = remaining.lastIndexOf("\n", TELEGRAM_TEXT_LIMIT);
            }
            if (splitIndex < TELEGRAM_TEXT_LIMIT / 2) {
                splitIndex = TELEGRAM_TEXT_LIMIT;
            }

            chunks.add(remaining.substring(0, splitIndex).trim());
            remaining = remaining.substring(splitIndex).trim();
        }

        if (!remaining.isBlank()) {
            chunks.add(remaining);
        }

        return chunks;
    }
}
