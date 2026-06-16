package com.example.trackingbot.service.telegram;

import com.example.trackingbot.service.alert.AlertBuilderService;
import com.example.trackingbot.service.alert.AlertService;
import com.example.trackingbot.service.analysis.AiPredictionService;
import com.example.trackingbot.service.analysis.SignalScoreService;
import com.example.trackingbot.service.chart.IdeaChartService;
import com.example.trackingbot.service.crypto.CryptoChartService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.daily.DailyMarketSummaryService;
import com.example.trackingbot.service.news.TelegramChannelNewsService;
import com.example.trackingbot.service.news.TelegramChannelNewsService.NewsPage;
import com.example.trackingbot.service.notification.NotificationHistoryService;
import com.example.trackingbot.service.portfolio.PortfolioService;
import com.example.trackingbot.service.subscription.SubscriptionService;
import com.example.trackingbot.service.watchlist.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramCallbackService {

    private static final Logger log = LoggerFactory.getLogger(TelegramCallbackService.class);

    private final TelegramMessageService telegramMessageService;
    private final TelegramKeyboardService telegramKeyboardService;
    private final CryptoPriceService cryptoPriceService;
    private final CryptoChartService cryptoChartService;
    private final WatchlistService watchlistService;
    private final AlertService alertService;
    private final AlertBuilderService alertBuilderService;
    private final PortfolioService portfolioService;
    private final DailyMarketSummaryService dailyMarketSummaryService;
    private final IdeaChartService ideaChartService;
    private final AiPredictionService aiPredictionService;
    private final SignalScoreService signalScoreService;
    private final TelegramChannelNewsService telegramChannelNewsService;
    private final SubscriptionService subscriptionService;
    private final NotificationHistoryService notificationHistoryService;

    public void handleCallbackQuery(String callbackQueryId, Long chatId, String callbackData) {
        if (callbackData == null || callbackData.isBlank()) {
            telegramMessageService.answerCallbackQuery(callbackQueryId, "");
            return;
        }

        String[] parts = callbackData.split(":");
        try {
            switch (parts[0]) {
                case "CHART" -> handleChartCallback(chatId, parts);
                case "IDEA_CHART" -> handleIdeaChartCallback(chatId, parts);
                case "AI_PREDICTION" -> handleAiPredictionCallback(chatId, parts);
                case "AI_CHART" -> handleAiChartCallback(chatId, parts);
                case "PRICE" -> handlePriceCallback(chatId, parts);
                case "SIGNAL" -> handleSignalCallback(chatId, parts);
                case "WATCH" -> handleWatchCallback(chatId, parts);
                case "UNWATCH" -> handleUnwatchCallback(chatId, parts);
                case "ALERT_PROMPT" -> handleAlertPromptCallback(chatId, parts);
                case "ALERT_BUILDER" -> handleAlertBuilderCallback(chatId, parts);
                case "NEWS_PAGE" -> handleNewsPageCallback(chatId, parts);
                case "MARKET_SYMBOL" -> handleMarketSymbolCallback(chatId, parts);
                case "MENU" -> handleMenuCallback(chatId, parts);
                default -> telegramMessageService.sendTextMessage(chatId, "Nut nay khong con duoc ho tro.");
            }
            telegramMessageService.answerCallbackQuery(callbackQueryId, "Done");
        } catch (Exception exception) {
            log.warn("Failed to handle callback {}", callbackData, exception);
            telegramMessageService.answerCallbackQuery(callbackQueryId, "Error");
            telegramMessageService.sendTextMessage(chatId, "Tam thoi khong xu ly duoc nut nay. Ban thu lai sau nhe.");
        }
    }

    private void handleChartCallback(Long chatId, String[] parts) {
        if (parts.length != 3) {
            telegramMessageService.sendTextMessage(chatId, cryptoChartService.getHelpMessage());
            return;
        }

        var chartImage = cryptoChartService.getChartImage(parts[1], parts[2]);
        telegramMessageService.sendPhoto(chatId, chartImage.imageUrl(), chartImage.caption());
    }

    private void handlePriceCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            telegramMessageService.sendTextMessage(chatId, cryptoPriceService.getHelpMessage());
            return;
        }

        String symbol = cryptoPriceService.normalizeSymbol(parts[1]);
        String response = cryptoPriceService.getPriceMessage(symbol);
        telegramMessageService.sendTextMessage(chatId, response, telegramKeyboardService.buildCryptoKeyboard(symbol));
    }

    private void handleSignalCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            telegramMessageService.sendTextMessage(chatId, signalScoreService.getHelpMessage());
            return;
        }

        telegramMessageService.sendTextMessage(chatId, signalScoreService.getSignalMessage(parts[1]));
    }

    private void handleIdeaChartCallback(Long chatId, String[] parts) {
        if (parts.length != 4) {
            telegramMessageService.sendTextMessage(chatId, ideaChartService.getHelpMessage());
            return;
        }

        String type = parts[1];
        String arguments = parts[2] + " " + parts[3];
        var chart = switch (type) {
            case "IDEA" -> ideaChartService.createIdeaChart(arguments);
            case "VOLUME" -> ideaChartService.createVolumeDeltaChart(arguments);
            case "BREAKOUT" -> ideaChartService.createBreakoutChart(arguments);
            case "TRENDLINE" -> ideaChartService.createTrendlineChart(arguments);
            case "ORDER_FLOW" -> ideaChartService.createOrderFlowChart(arguments);
            default -> throw new IllegalArgumentException("Unsupported idea chart callback: " + type);
        };

        telegramMessageService.sendPhotoFile(
                chatId,
                chart.imagePath(),
                chart.caption(),
                telegramKeyboardService.buildIdeaChartKeyboard(chart.symbol(), chart.interval(), type)
        );
    }

    private void handleAiPredictionCallback(Long chatId, String[] parts) {
        if (parts.length != 3) {
            telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
            return;
        }

        var quotaDecision = subscriptionService.consumeAiQuota(chatId);
        if (!quotaDecision.allowed()) {
            telegramMessageService.sendTextMessage(chatId, subscriptionService.buildQuotaExceededMessage(quotaDecision));
            return;
        }

        String arguments = parts[1] + " " + parts[2];
        telegramMessageService.sendTextMessage(chatId, "Dang tao AI quant analysis bang GPT-5 mini, doi minh mot chut...");
        telegramMessageService.sendTextMessage(chatId, aiPredictionService.getAiPredictionMessage(arguments));
    }

    private void handleAiChartCallback(Long chatId, String[] parts) {
        if (parts.length != 3) {
            telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
            return;
        }

        var quotaDecision = subscriptionService.consumeAiQuota(chatId);
        if (!quotaDecision.allowed()) {
            telegramMessageService.sendTextMessage(chatId, subscriptionService.buildQuotaExceededMessage(quotaDecision));
            return;
        }

        String arguments = parts[1] + " " + parts[2];
        telegramMessageService.sendTextMessage(chatId, "Dang tao AI Quant Map bang GPT-5 mini, doi minh mot chut...");
        var chart = aiPredictionService.createAiChart(arguments);
        telegramMessageService.sendPhotoFile(
                chatId,
                chart.imagePath(),
                chart.caption(),
                telegramKeyboardService.buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "AI_CHART")
        );
    }

    private void handleWatchCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            telegramMessageService.sendTextMessage(chatId, watchlistService.getHelpMessage());
            return;
        }

        telegramMessageService.sendTextMessage(chatId, watchlistService.addToWatchlist(chatId, parts[1]));
    }

    private void handleUnwatchCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            telegramMessageService.sendTextMessage(chatId, watchlistService.getHelpMessage());
            return;
        }

        telegramMessageService.sendTextMessage(chatId, watchlistService.removeFromWatchlist(chatId, parts[1]));
        sendWatchlistMessage(chatId);
    }

    private void handleAlertPromptCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
            return;
        }

        telegramMessageService.sendTextMessage(chatId, """
                Tao alert cho %s:

                Vi du:
                /alert %s > 70000
                /alert %s < 60000
                """.formatted(parts[1], parts[1], parts[1]));
    }

    private void handleAlertBuilderCallback(Long chatId, String[] parts) {
        if (parts.length < 2) {
            sendAlertBuilderStart(chatId);
            return;
        }

        switch (parts[1]) {
            case "START" -> sendAlertBuilderStart(chatId);
            case "SYMBOL" -> handleAlertBuilderSymbolStep(chatId, parts);
            case "OPERATOR" -> handleAlertBuilderOperatorStep(chatId, parts);
            case "PERCENT" -> handleAlertBuilderPercentStep(chatId, parts);
            case "CUSTOM" -> handleAlertBuilderCustomStep(chatId, parts);
            default -> telegramMessageService.sendTextMessage(chatId, "Lua chon alert builder khong hop le.");
        }
    }

    private void sendAlertBuilderStart(Long chatId) {
        telegramMessageService.sendTextMessage(
                chatId,
                alertBuilderService.getStartMessage(),
                alertBuilderService.buildSymbolKeyboard()
        );
    }

    private void handleAlertBuilderSymbolStep(Long chatId, String[] parts) {
        if (parts.length != 3 || !alertBuilderService.isSupportedSymbol(parts[2])) {
            sendAlertBuilderStart(chatId);
            return;
        }

        telegramMessageService.sendTextMessage(
                chatId,
                alertBuilderService.getOperatorMessage(parts[2]),
                alertBuilderService.buildOperatorKeyboard(parts[2])
        );
    }

    private void handleAlertBuilderOperatorStep(Long chatId, String[] parts) {
        if (parts.length != 4
                || !alertBuilderService.isSupportedSymbol(parts[2])
                || !alertBuilderService.isSupportedOperatorCode(parts[3])) {
            telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
            return;
        }

        telegramMessageService.sendTextMessage(
                chatId,
                alertBuilderService.getPercentMessage(parts[2], parts[3]),
                alertBuilderService.buildPercentKeyboard(parts[2], parts[3])
        );
    }

    private void handleAlertBuilderPercentStep(Long chatId, String[] parts) {
        if (parts.length != 5
                || !alertBuilderService.isSupportedSymbol(parts[2])
                || !alertBuilderService.isSupportedOperatorCode(parts[3])) {
            telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
            return;
        }

        try {
            int percent = Integer.parseInt(parts[4]);
            telegramMessageService.sendTextMessage(
                    chatId,
                    alertBuilderService.createPercentAlert(chatId, parts[2], parts[3], percent)
            );
        } catch (NumberFormatException exception) {
            telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
        }
    }

    private void handleAlertBuilderCustomStep(Long chatId, String[] parts) {
        if (parts.length != 4
                || !alertBuilderService.isSupportedSymbol(parts[2])
                || !alertBuilderService.isSupportedOperatorCode(parts[3])) {
            telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
            return;
        }

        telegramMessageService.sendTextMessage(
                chatId,
                alertBuilderService.getCustomPriceMessage(parts[2], parts[3])
        );
    }

    private void handleNewsPageCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            sendNewsPage(chatId, 1);
            return;
        }

        try {
            sendNewsPage(chatId, Integer.parseInt(parts[1]));
        } catch (NumberFormatException exception) {
            sendNewsPage(chatId, 1);
        }
    }

    private void handleMarketSymbolCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            telegramMessageService.sendTextMessage(chatId, "Chon coin:", telegramKeyboardService.buildMarketSymbolKeyboard());
            return;
        }

        String symbol = cryptoPriceService.normalizeSymbol(parts[1]);
        telegramMessageService.sendTextMessage(
                chatId,
                "%s menu".formatted(symbol),
                telegramKeyboardService.buildSymbolDashboardKeyboard(symbol)
        );
    }

    private void handleMenuCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            sendMainMenu(chatId);
            return;
        }

        switch (parts[1]) {
            case "MAIN" -> sendMainMenu(chatId);
            case "MARKET" -> telegramMessageService.sendTextMessage(chatId, "Chon coin:", telegramKeyboardService.buildMarketSymbolKeyboard());
            case "CHARTS" -> telegramMessageService.sendTextMessage(chatId, "Chon coin de xem chart 7d:", telegramKeyboardService.buildChartSymbolKeyboard());
            case "WATCHLIST" -> sendWatchlistMessage(chatId);
            case "ALERTS" -> telegramMessageService.sendTextMessage(
                    chatId,
                    alertService.getUserAlertsMessage(chatId),
                    telegramKeyboardService.buildAlertMenuKeyboard()
            );
            case "PORTFOLIO" -> telegramMessageService.sendTextMessage(
                    chatId,
                    portfolioService.getPortfolioMessage(chatId),
                    telegramKeyboardService.buildPortfolioMenuKeyboard()
            );
            case "NEWS" -> sendNewsPage(chatId, 1);
            case "AI" -> telegramMessageService.sendTextMessage(chatId, "Chon coin de tao AI analysis:", telegramKeyboardService.buildAiSymbolKeyboard());
            case "SETTINGS" -> telegramMessageService.sendTextMessage(chatId, "Settings:", telegramKeyboardService.buildSettingsKeyboard());
            case "USAGE" -> telegramMessageService.sendTextMessage(chatId, subscriptionService.getUsageMessage(chatId), telegramKeyboardService.buildSettingsKeyboard());
            case "NOTIFICATIONS" -> telegramMessageService.sendTextMessage(chatId, notificationHistoryService.getMyNotificationsMessage(chatId));
            case "DAILY_ON" -> telegramMessageService.sendTextMessage(chatId, dailyMarketSummaryService.enableDailySummary(chatId), telegramKeyboardService.buildSettingsKeyboard());
            case "DAILY_OFF" -> telegramMessageService.sendTextMessage(chatId, dailyMarketSummaryService.disableDailySummary(chatId), telegramKeyboardService.buildSettingsKeyboard());
            case "WATCH_UPDATES_ON" -> telegramMessageService.sendTextMessage(chatId, watchlistService.enableWatchUpdates(chatId), telegramKeyboardService.buildSettingsKeyboard());
            case "WATCH_UPDATES_OFF" -> telegramMessageService.sendTextMessage(chatId, watchlistService.disableWatchUpdates(chatId), telegramKeyboardService.buildSettingsKeyboard());
            default -> sendMainMenu(chatId);
        }
    }

    private void sendMainMenu(Long chatId) {
        telegramMessageService.sendTextMessage(chatId, "Main menu:", telegramKeyboardService.buildMainMenuKeyboard());
    }

    public void sendNewsPage(Long chatId, int page) {
        NewsPage newsPage = telegramChannelNewsService.getLatestNewsPage(page);
        if (newsPage.totalPages() <= 1) {
            telegramMessageService.sendTextMessage(chatId, newsPage.message());
            return;
        }

        telegramMessageService.sendTextMessage(chatId, newsPage.message(), telegramKeyboardService.buildNewsKeyboard(newsPage));
    }

    public void sendWatchlistMessage(Long chatId) {
        List<String> symbols = watchlistService.getUserSymbols(chatId);
        String message = watchlistService.getWatchlistMessage(chatId);
        if (symbols.isEmpty()) {
            telegramMessageService.sendTextMessage(chatId, message, telegramKeyboardService.buildEmptyWatchlistKeyboard());
            return;
        }

        telegramMessageService.sendTextMessage(chatId, message, telegramKeyboardService.buildWatchlistKeyboard(symbols));
    }
}
