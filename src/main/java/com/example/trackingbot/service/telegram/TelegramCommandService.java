package com.example.trackingbot.service.telegram;

import com.example.trackingbot.service.audit.CommandLogService;
import com.example.trackingbot.service.command.CommandHandleResult;
import com.example.trackingbot.service.command.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramCommandService {

    private final TelegramMessageService telegramMessageService;
    private final TelegramKeyboardService telegramKeyboardService;
    private final TelegramCallbackService telegramCallbackService;
    private final TelegramHelpService telegramHelpService;
    private final NaturalLanguageCommandService naturalLanguageCommandService;
    private final UserCommandRateLimiter userCommandRateLimiter;
    private final CommandLogService commandLogService;
    private final List<CommandHandler> commandHandlers;

    public void handleTextMessage(Long chatId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String commandText = text.trim();
        String command = extractCommandName(commandText);
        long startedAt = System.nanoTime();
        boolean success = true;
        String errorMessage = null;

        try {
            if (isHelpCommand(commandText)) {
                telegramMessageService.sendTextMessage(
                        chatId,
                        telegramHelpService.getMainHelpMessage(),
                        telegramKeyboardService.buildMainMenuKeyboard()
                );
                return;
            }

            var rateLimitResult = userCommandRateLimiter.checkAllowed(chatId, commandText);
            if (!rateLimitResult.allowed()) {
                success = false;
                errorMessage = "RATE_LIMIT: %s max %d/%s retry after %ds".formatted(
                        rateLimitResult.ruleName(),
                        rateLimitResult.maxRequests(),
                        rateLimitResult.windowLabel(),
                        rateLimitResult.retryAfterSeconds()
                );
                telegramMessageService.sendTextMessage(chatId, buildRateLimitMessage(rateLimitResult));
                return;
            }

            CommandHandleResult commandResult = handleSlashCommand(chatId, commandText);
            if (commandResult.handled()) {
                success = commandResult.success();
                errorMessage = commandResult.errorMessage();
                return;
            }

            if (naturalLanguageCommandService.handle(chatId, commandText)) {
                return;
            }

            telegramMessageService.sendTextMessage(chatId, telegramHelpService.getUnknownCommandMessage());
        } catch (Exception exception) {
            success = false;
            errorMessage = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            throw exception;
        } finally {
            commandLogService.record(
                    chatId,
                    command,
                    success,
                    errorMessage,
                    durationMillisSince(startedAt)
            );
        }
    }

    public void handleCallbackQuery(String callbackQueryId, Long chatId, String callbackData) {
        telegramCallbackService.handleCallbackQuery(callbackQueryId, chatId, callbackData);
    }

    private boolean isHelpCommand(String commandText) {
        return "/start".equalsIgnoreCase(commandText) || "/help".equalsIgnoreCase(commandText);
    }

    private CommandHandleResult handleSlashCommand(Long chatId, String commandText) {
        for (CommandHandler handler : commandHandlers) {
            CommandHandleResult result = handler.handle(chatId, commandText);
            if (result.handled()) {
                return result;
            }
        }

        return CommandHandleResult.notHandled();
    }

    private String extractCommandName(String commandText) {
        return commandText.trim().split("\\s+", 2)[0].toLowerCase();
    }

    private long durationMillisSince(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String buildRateLimitMessage(UserCommandRateLimiter.RateLimitResult result) {
        return """
                Ban dang gui lenh qua nhanh.

                Gioi han: %s toi da %d lan/%s.
                Thu lai sau khoang %d giay.
                """.formatted(
                result.ruleName(),
                result.maxRequests(),
                result.windowLabel(),
                result.retryAfterSeconds()
        );
    }
}
