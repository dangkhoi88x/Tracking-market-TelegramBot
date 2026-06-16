package com.example.trackingbot.service.command;

import com.example.trackingbot.service.admin.AdminObservabilityService;
import com.example.trackingbot.service.subscription.SubscriptionService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@RequiredArgsConstructor
public class AdminCommandHandler implements CommandHandler {

    private final TelegramMessageService telegramMessageService;
    private final AdminObservabilityService adminObservabilityService;
    private final SubscriptionService subscriptionService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/admin_health")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getHealthMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/admin_metrics")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getMetricsMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/admin_top_commands")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getTopCommandsMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/admin_errors")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getErrorsMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/admin_users")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getUsersMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/admin_set_plan")) {
            telegramMessageService.sendTextMessage(
                    chatId,
                    subscriptionService.setPlan(chatId, extractCommandArgument(commandText))
            );
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }
}
