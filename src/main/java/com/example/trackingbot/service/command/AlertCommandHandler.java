package com.example.trackingbot.service.command;

import com.example.trackingbot.service.alert.AlertBuilderService;
import com.example.trackingbot.service.alert.AlertService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@RequiredArgsConstructor
public class AlertCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(AlertCommandHandler.class);

    private final TelegramMessageService telegramMessageService;
    private final CryptoPriceService cryptoPriceService;
    private final AlertService alertService;
    private final AlertBuilderService alertBuilderService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/notif")) {
            handleNotification(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/myalerts")) {
            telegramMessageService.sendTextMessage(chatId, alertService.getUserAlertsMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/alert_builder")) {
            telegramMessageService.sendTextMessage(
                    chatId,
                    alertBuilderService.getStartMessage(),
                    alertBuilderService.buildSymbolKeyboard()
            );
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/delete_alert")) {
            String alertId = extractCommandArgument(commandText);
            telegramMessageService.sendTextMessage(chatId, alertService.deleteAlert(chatId, alertId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/alert")) {
            handleAlert(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }

    private void handleNotification(Long chatId, String commandText) {
        String arguments = extractCommandArgument(commandText);
        if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
            telegramMessageService.sendTextMessage(chatId, getNotificationHelpMessage());
            return;
        }

        try {
            telegramMessageService.sendTextMessage(chatId, alertService.createNotification(chatId, arguments));
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, getNotificationHelpMessage());
        } catch (Exception exception) {
            log.warn("Failed to create notification for arguments {}", arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc notification.

                    Ban thu lai sau nhe.
                    """);
        }
    }

    private void handleAlert(Long chatId, String commandText) {
        String arguments = extractCommandArgument(commandText);
        if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
            telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
            return;
        }

        try {
            telegramMessageService.sendTextMessage(chatId, alertService.createAlert(chatId, arguments));
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
        } catch (Exception exception) {
            log.warn("Failed to create alert for arguments {}", arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc alert.

                    Ban thu lai sau nhe.
                    """);
        }
    }

    private String getNotificationHelpMessage() {
        return """
                Cach dung:
                /notif BTC 100000

                Bot se nhac khi coin cham muc gia nay.
                """;
    }
}
