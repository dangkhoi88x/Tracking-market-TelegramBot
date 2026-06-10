package com.example.trackingbot.service;

import org.springframework.stereotype.Service;

@Service
public class TelegramCommandService {

    private final TelegramMessageService telegramMessageService;

    public TelegramCommandService(TelegramMessageService telegramMessageService) {
        this.telegramMessageService = telegramMessageService;
    }

    public void handleTextMessage(Long chatId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        if ("/start".equalsIgnoreCase(text.trim())) {
            telegramMessageService.sendTextMessage(chatId, """
                    Xin chao! Minh la bot theo doi thi truong.

                    Lenh hien co:
                    /start - xem huong dan

                    Sap toi minh se them:
                    /crypto BTC
                    /stock VNM
                    """);
        }
    }
}
