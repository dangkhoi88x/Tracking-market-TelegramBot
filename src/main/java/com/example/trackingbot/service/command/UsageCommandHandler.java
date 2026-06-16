package com.example.trackingbot.service.command;

import com.example.trackingbot.service.notification.NotificationHistoryService;
import com.example.trackingbot.service.subscription.SubscriptionService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
@RequiredArgsConstructor
public class UsageCommandHandler implements CommandHandler {

    private final TelegramMessageService telegramMessageService;
    private final SubscriptionService subscriptionService;
    private final NotificationHistoryService notificationHistoryService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/my_usage")) {
            telegramMessageService.sendTextMessage(chatId, subscriptionService.getUsageMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/my_notifications")) {
            telegramMessageService.sendTextMessage(chatId, notificationHistoryService.getMyNotificationsMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }
}
