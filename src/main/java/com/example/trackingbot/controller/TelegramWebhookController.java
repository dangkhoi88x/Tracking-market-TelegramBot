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
//Controller không xử lý command.
//Controller chỉ nhận webhook, phân loại update, rồi chuyển xuống service.
public class TelegramWebhookController {

    private final TelegramCommandService telegramCommandService; //xử lý text command hoặc callback button
    private final TelegramMessageService telegramMessageService; //Dùng để check webhook secret.
    private final TelegramUserService telegramUserService; //tạo user nếu chưa có trong DB.

    @PostMapping("/webhook")
    // nhan vao body json tu telegram va spring map object TelegramUpdate
    //Telegram có thể gửi kèm header secret để app  confirm request này đúng là từ Telegram.
    public ResponseEntity<Void> receiveUpdate(
            @RequestBody TelegramUpdate update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretHeader
    ) {
        if (!telegramMessageService.isValidSecret(secretHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // xu ly khi user click button thay vi viet command
        //controller se lay chatid or tao user moi roif handle
        if (update.callbackQuery() != null && update.callbackQuery().message() != null) {
            telegramUserService.getOrCreateUser(update.callbackQuery().message().chat().id());
            telegramCommandService.handleCallbackQuery(
                    update.callbackQuery().id(), //callback de telegram biet da nhan button click
                    update.callbackQuery().message().chat().id(), // tra loi cho user nao
                    update.callbackQuery().data() // du lieu button
            );
            // CHART:BTC:7d . WATCH:BTC .AI_PREDICTION:BTC:4h
            return ResponseEntity.ok().build();
        }

        // xu ly text message command /crypto BTC , gia btc
        if (update.message() != null && update.message().chat() != null) {
            telegramUserService.getOrCreateUser(update.message().chat().id());
            // truyen xuong service chatID,text
            telegramCommandService.handleTextMessage(
                    update.message().chat().id(),
                    update.message().text()
            );
        }
// tra ve cho telegram biet webhook da nhan request
        return ResponseEntity.ok().build();
    }
}
