package com.example.trackingbot.service.command;

import com.example.trackingbot.service.telegram.TelegramCallbackService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(70)
@RequiredArgsConstructor
public class NewsCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(NewsCommandHandler.class);

    private final TelegramMessageService telegramMessageService;
    private final TelegramCallbackService telegramCallbackService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (!isCommand(commandText, "/tintuc")) {
            return CommandHandleResult.notHandled();
        }

        try {
            telegramCallbackService.sendNewsPage(chatId, 1);
        } catch (Exception exception) {
            log.warn("Failed to get news from Telegram channel", exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong lay duoc tin tuc tu @vncointele.

                    Ban thu lai sau nhe.
                    """);
        }

        return CommandHandleResult.handledSuccessfully();
    }
}
