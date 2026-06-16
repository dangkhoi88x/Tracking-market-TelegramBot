package com.example.trackingbot.service.command;

import com.example.trackingbot.service.chart.IdeaChartService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.telegram.TelegramKeyboardService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@RequiredArgsConstructor
public class ChartCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ChartCommandHandler.class);

    private final TelegramMessageService telegramMessageService;
    private final TelegramKeyboardService telegramKeyboardService;
    private final CryptoPriceService cryptoPriceService;
    private final IdeaChartService ideaChartService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/idea")) {
            handleIdea(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/chart_volume")) {
            handleChart(chatId, commandText, "volume delta", "VOLUME");
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/chart_breakout")) {
            handleChart(chatId, commandText, "breakout confirmation", "BREAKOUT");
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/chart_trendline")) {
            handleChart(chatId, commandText, "trendline", "TRENDLINE");
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/chart_orderflow")) {
            handleChart(chatId, commandText, "order flow", "ORDER_FLOW");
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }

    private void handleIdea(Long chatId, String commandText) {
        String arguments = extractCommandArgument(commandText);
        if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
            telegramMessageService.sendTextMessage(chatId, ideaChartService.getHelpMessage());
            return;
        }

        try {
            telegramMessageService.sendTextMessage(chatId, "Dang tao idea chart, doi minh mot chut...");
            var chart = ideaChartService.createIdeaChart(arguments);
            telegramMessageService.sendPhotoFile(
                    chatId,
                    chart.imagePath(),
                    chart.caption(),
                    telegramKeyboardService.buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "IDEA")
            );
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, ideaChartService.getHelpMessage());
        } catch (Exception exception) {
            log.warn("Failed to create idea chart for arguments {}", arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc idea chart.

                    Ban kiem tra Node/Playwright hoac thu lai sau nhe.
                    """);
        }
    }

    private void handleChart(Long chatId, String commandText, String label, String keyboardType) {
        String arguments = extractCommandArgument(commandText);
        if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
            telegramMessageService.sendTextMessage(chatId, helpMessageFor(keyboardType));
            return;
        }

        try {
            telegramMessageService.sendTextMessage(chatId, "Dang tao " + label + " chart, doi minh mot chut...");
            var chart = switch (keyboardType) {
                case "VOLUME" -> ideaChartService.createVolumeDeltaChart(arguments);
                case "BREAKOUT" -> ideaChartService.createBreakoutChart(arguments);
                case "TRENDLINE" -> ideaChartService.createTrendlineChart(arguments);
                case "ORDER_FLOW" -> ideaChartService.createOrderFlowChart(arguments);
                default -> throw new IllegalArgumentException("Unsupported chart type: " + keyboardType);
            };
            telegramMessageService.sendPhotoFile(
                    chatId,
                    chart.imagePath(),
                    chart.caption(),
                    telegramKeyboardService.buildIdeaChartKeyboard(chart.symbol(), chart.interval(), keyboardType)
            );
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, helpMessageFor(keyboardType));
        } catch (Exception exception) {
            log.warn("Failed to create {} chart for arguments {}", label, arguments, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc %s chart.

                    Ban thu lai sau nhe.
                    """.formatted(label));
        }
    }

    private String helpMessageFor(String keyboardType) {
        return switch (keyboardType) {
            case "VOLUME" -> ideaChartService.getVolumeDeltaHelpMessage();
            case "BREAKOUT" -> ideaChartService.getBreakoutHelpMessage();
            case "TRENDLINE" -> ideaChartService.getTrendlineHelpMessage();
            case "ORDER_FLOW" -> ideaChartService.getOrderFlowHelpMessage();
            default -> ideaChartService.getHelpMessage();
        };
    }
}
