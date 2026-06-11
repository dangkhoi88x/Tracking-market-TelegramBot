package com.example.trackingbot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramAsyncService {

    private static final Logger log = LoggerFactory.getLogger(TelegramAsyncService.class);

    private final TelegramMessageService telegramMessageService;

    @Async("telegramTaskExecutor")
    public void sendTextMessage(Long chatId, String message) {
        try {
            telegramMessageService.sendTextMessage(chatId, message);
        } catch (Exception exception) {
            log.warn("Failed to send async Telegram message to chat {}", chatId, exception);
        }
    }
}
