package com.example.trackingbot.controller;

import com.example.trackingbot.dto.response.TelegramUpdate;
import com.example.trackingbot.service.telegram.TelegramCommandService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import com.example.trackingbot.service.telegram.TelegramUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramCommandService telegramCommandService;
    private final TelegramMessageService telegramMessageService;
    private final TelegramUserService telegramUserService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveUpdate(
            @RequestBody TelegramUpdate update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretHeader
    ) {
        if (!telegramMessageService.isValidSecret(secretHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (update.callbackQuery() != null && update.callbackQuery().message() != null) {
            telegramUserService.getOrCreateUser(update.callbackQuery().message().chat().id());
            telegramCommandService.handleCallbackQuery(
                    update.callbackQuery().id(),
                    update.callbackQuery().message().chat().id(),
                    update.callbackQuery().data()
            );
            return ResponseEntity.ok().build();
        }

        if (update.message() != null && update.message().chat() != null) {
            telegramUserService.getOrCreateUser(update.message().chat().id());
            telegramCommandService.handleTextMessage(
                    update.message().chat().id(),
                    update.message().text()
            );
        }

        return ResponseEntity.ok().build();
    }
}
