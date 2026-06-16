package com.example.trackingbot.service.command;

import com.example.trackingbot.service.analysis.AiPredictionService;
import com.example.trackingbot.service.analysis.SignalScoreService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.subscription.SubscriptionService;
import com.example.trackingbot.service.telegram.TelegramKeyboardService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class AiCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(AiCommandHandler.class);

    private final TelegramMessageService telegramMessageService;
    private final TelegramKeyboardService telegramKeyboardService;
    private final CryptoPriceService cryptoPriceService;
    private final AiPredictionService aiPredictionService;
    private final SignalScoreService signalScoreService;
    private final SubscriptionService subscriptionService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/ai")) {
            return handleAi(chatId, commandText);
        }

        if (isCommand(commandText, "/ai_chart")) {
            return handleAiChart(chatId, commandText);
        }

        if (isCommand(commandText, "/signal")) {
            handleSignal(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }

    private CommandHandleResult handleAi(Long chatId, String commandText) {
        String arguments = extractCommandArgument(commandText);
        if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
            telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
            return CommandHandleResult.handledSuccessfully();
        }

        var quotaDecision = subscriptionService.consumeAiQuota(chatId);
        if (!quotaDecision.allowed()) {
            telegramMessageService.sendTextMessage(chatId, subscriptionService.buildQuotaExceededMessage(quotaDecision));
            return CommandHandleResult.failed("AI_QUOTA_EXCEEDED: %s %d/%d".formatted(
                    quotaDecision.plan(),
                    quotaDecision.used(),
                    quotaDecision.limit()
            ));
        }

        try {
            telegramMessageService.sendTextMessage(chatId, "Dang tao AI quant analysis bang GPT-5 mini, doi minh mot chut...");
            telegramMessageService.sendTextMessage(chatId, aiPredictionService.getAiPredictionMessage(arguments));
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
        } catch (IllegalStateException exception) {
            log.warn("Failed to create AI prediction for arguments {}", arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Chua cau hinh OpenAI API key hoac AI response khong hop le.

                    Ban can them env:
                    OPENAI_API_KEY=your_api_key

                    Sau do restart Spring Boot va thu lai /ai BTC.
                    """);
        } catch (Exception exception) {
            log.warn("Failed to create AI prediction for arguments {}", arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc AI analysis.

                    Ban thu lai sau nhe.
                    """);
        }

        return CommandHandleResult.handledSuccessfully();
    }

    private CommandHandleResult handleAiChart(Long chatId, String commandText) {
        String arguments = extractCommandArgument(commandText);
        if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
            telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
            return CommandHandleResult.handledSuccessfully();
        }

        var quotaDecision = subscriptionService.consumeAiQuota(chatId);
        if (!quotaDecision.allowed()) {
            telegramMessageService.sendTextMessage(chatId, subscriptionService.buildQuotaExceededMessage(quotaDecision));
            return CommandHandleResult.failed("AI_QUOTA_EXCEEDED: %s %d/%d".formatted(
                    quotaDecision.plan(),
                    quotaDecision.used(),
                    quotaDecision.limit()
            ));
        }

        try {
            telegramMessageService.sendTextMessage(chatId, "Dang tao AI Quant Map bang GPT-5 mini, doi minh mot chut...");
            var chart = aiPredictionService.createAiChart(arguments);
            telegramMessageService.sendPhotoFile(
                    chatId,
                    chart.imagePath(),
                    chart.caption(),
                    telegramKeyboardService.buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "AI_CHART")
            );
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
        } catch (IllegalStateException exception) {
            log.warn("Failed to create AI chart for arguments {}", arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Chua cau hinh OpenAI API key, AI response khong hop le, hoac renderer khong tao duoc chart.

                    Ban can them env:
                    OPENAI_API_KEY=your_api_key

                    Sau do restart Spring Boot va thu lai /ai_chart BTC.
                    """);
        } catch (Exception exception) {
            log.warn("Failed to create AI chart for arguments {}", arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc AI Quant Map.

                    Ban thu lai sau nhe.
                    """);
        }

        return CommandHandleResult.handledSuccessfully();
    }

    private void handleSignal(Long chatId, String commandText) {
        String arguments = extractCommandArgument(commandText);
        if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
            telegramMessageService.sendTextMessage(chatId, signalScoreService.getHelpMessage());
            return;
        }

        try {
            telegramMessageService.sendTextMessage(chatId, signalScoreService.getSignalMessage(arguments));
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, signalScoreService.getHelpMessage());
        } catch (Exception exception) {
            log.warn("Failed to create signal score for arguments {}", arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc signal score.

                    Ban thu lai sau nhe.
                    """);
        }
    }
}
