package com.example.trackingbot.service.command;

import com.example.trackingbot.service.daily.DailyMarketSummaryService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(60)
@RequiredArgsConstructor
public class DailyCommandHandler implements CommandHandler {

    private final TelegramMessageService telegramMessageService;
    private final DailyMarketSummaryService dailyMarketSummaryService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/daily_on")) {
            telegramMessageService.sendTextMessage(chatId, dailyMarketSummaryService.enableDailySummary(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/daily_off")) {
            telegramMessageService.sendTextMessage(chatId, dailyMarketSummaryService.disableDailySummary(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }
}
