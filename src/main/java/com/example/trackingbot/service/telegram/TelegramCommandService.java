package com.example.trackingbot.service.telegram;

import com.example.trackingbot.dto.request.InlineKeyboardButton;
import com.example.trackingbot.dto.request.InlineKeyboardMarkup;
import com.example.trackingbot.service.admin.AdminObservabilityService;
import com.example.trackingbot.service.alert.AlertBuilderService;
import com.example.trackingbot.service.alert.AlertService;
import com.example.trackingbot.service.alert.NaturalLanguageAlertParser;
import com.example.trackingbot.service.analysis.AiPredictionService;
import com.example.trackingbot.service.analysis.SignalScoreService;
import com.example.trackingbot.service.audit.CommandLogService;
import com.example.trackingbot.service.chart.IdeaChartService;
import com.example.trackingbot.service.crypto.CryptoChartService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.crypto.TrendingCryptoService;
import com.example.trackingbot.service.crypto.UsdtRateService;
import com.example.trackingbot.service.crypto.ValueConversionService;
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
public class TelegramCommandService {

    private static final Logger log = LoggerFactory.getLogger(TelegramCommandService.class);

    private final TelegramMessageService telegramMessageService;
    private final CryptoPriceService cryptoPriceService;
    private final CryptoChartService cryptoChartService;
    private final WatchlistService watchlistService;
    private final AlertService alertService;
    private final AlertBuilderService alertBuilderService;
    private final NaturalLanguageAlertParser naturalLanguageAlertParser;
    private final TrendingCryptoService trendingCryptoService;
    private final PortfolioService portfolioService;
    private final DailyMarketSummaryService dailyMarketSummaryService;
    private final ValueConversionService valueConversionService;
    private final UsdtRateService usdtRateService;
    private final IdeaChartService ideaChartService;
    private final AiPredictionService aiPredictionService;
    private final SignalScoreService signalScoreService;
    private final AdminObservabilityService adminObservabilityService;
    private final UserCommandRateLimiter userCommandRateLimiter;
    private final TelegramChannelNewsService telegramChannelNewsService;
    private final CommandLogService commandLogService;
    private final SubscriptionService subscriptionService;
    private final NotificationHistoryService notificationHistoryService;
    private final NaturalLanguageCommandParser naturalLanguageCommandParser;

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

        if ("/start".equalsIgnoreCase(commandText) || "/help".equalsIgnoreCase(commandText)) {
            telegramMessageService.sendTextMessage(chatId, getMainHelpMessage(), buildMainMenuKeyboard());
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

        if (isCommand(commandText, "/idea")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getHelpMessage());
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, "Dang tao idea chart, doi minh mot chut...");
                var ideaChart = ideaChartService.createIdeaChart(arguments);
                telegramMessageService.sendPhotoFile(
                        chatId,
                        ideaChart.imagePath(),
                        ideaChart.caption(),
                        buildIdeaChartKeyboard(ideaChart.symbol(), ideaChart.interval(), "IDEA")
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
            return;
        }

        if (isCommand(commandText, "/chart_volume")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getVolumeDeltaHelpMessage());
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, "Dang tao volume delta chart, doi minh mot chut...");
                var chart = ideaChartService.createVolumeDeltaChart(arguments);
                telegramMessageService.sendPhotoFile(
                        chatId,
                        chart.imagePath(),
                        chart.caption(),
                        buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "VOLUME")
                );
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getVolumeDeltaHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to create volume delta chart for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong tao duoc volume delta chart.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/chart_breakout")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getBreakoutHelpMessage());
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, "Dang tao breakout confirmation chart, doi minh mot chut...");
                var chart = ideaChartService.createBreakoutChart(arguments);
                telegramMessageService.sendPhotoFile(
                        chatId,
                        chart.imagePath(),
                        chart.caption(),
                        buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "BREAKOUT")
                );
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getBreakoutHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to create breakout chart for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong tao duoc breakout chart.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/chart_trendline")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getTrendlineHelpMessage());
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, "Dang tao trendline chart, doi minh mot chut...");
                var chart = ideaChartService.createTrendlineChart(arguments);
                telegramMessageService.sendPhotoFile(
                        chatId,
                        chart.imagePath(),
                        chart.caption(),
                        buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "TRENDLINE")
                );
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getTrendlineHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to create trendline chart for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong tao duoc trendline chart.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/chart_orderflow")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getOrderFlowHelpMessage());
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, "Dang tao order flow chart, doi minh mot chut...");
                var chart = ideaChartService.createOrderFlowChart(arguments);
                telegramMessageService.sendPhotoFile(
                        chatId,
                        chart.imagePath(),
                        chart.caption(),
                        buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "ORDER_FLOW")
                );
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, ideaChartService.getOrderFlowHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to create order flow chart for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong tao duoc order flow chart.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/ai")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
                return;
            }

            try {
                var quotaDecision = subscriptionService.consumeAiQuota(chatId);
                if (!quotaDecision.allowed()) {
                    success = false;
                    errorMessage = "AI_QUOTA_EXCEEDED: %s %d/%d".formatted(
                            quotaDecision.plan(),
                            quotaDecision.used(),
                            quotaDecision.limit()
                    );
                    telegramMessageService.sendTextMessage(chatId, subscriptionService.buildQuotaExceededMessage(quotaDecision));
                    return;
                }

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
            return;
        }

        if (isCommand(commandText, "/ai_chart")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
                return;
            }

            try {
                var quotaDecision = subscriptionService.consumeAiQuota(chatId);
                if (!quotaDecision.allowed()) {
                    success = false;
                    errorMessage = "AI_QUOTA_EXCEEDED: %s %d/%d".formatted(
                            quotaDecision.plan(),
                            quotaDecision.used(),
                            quotaDecision.limit()
                    );
                    telegramMessageService.sendTextMessage(chatId, subscriptionService.buildQuotaExceededMessage(quotaDecision));
                    return;
                }

                telegramMessageService.sendTextMessage(chatId, "Dang tao AI Quant Map bang GPT-5 mini, doi minh mot chut...");
                var chart = aiPredictionService.createAiChart(arguments);
                telegramMessageService.sendPhotoFile(
                        chatId,
                        chart.imagePath(),
                        chart.caption(),
                        buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "AI_CHART")
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
            return;
        }

        if (isCommand(commandText, "/signal")) {
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
            return;
        }

        if (isCommand(commandText, "/admin_health")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getHealthMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/admin_metrics")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getMetricsMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/admin_top_commands")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getTopCommandsMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/admin_errors")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getErrorsMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/admin_users")) {
            telegramMessageService.sendTextMessage(chatId, adminObservabilityService.getUsersMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/admin_set_plan")) {
            telegramMessageService.sendTextMessage(
                    chatId,
                    subscriptionService.setPlan(chatId, extractCommandArgument(commandText))
            );
            return;
        }

        if (isCommand(commandText, "/my_usage")) {
            telegramMessageService.sendTextMessage(chatId, subscriptionService.getUsageMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/my_notifications")) {
            telegramMessageService.sendTextMessage(chatId, notificationHistoryService.getMyNotificationsMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/usdt")) {
            try {
                telegramMessageService.sendTextMessage(chatId, usdtRateService.getUsdtMessage());
            } catch (Exception exception) {
                log.warn("Failed to get USDT/VND rate", exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong lay duoc gia USDT/VND.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/val")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, valueConversionService.getHelpMessage());
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, valueConversionService.getValueMessage(arguments));
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, valueConversionService.getHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to calculate value for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong tinh duoc value.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/notif")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, """
                        Cach dung:
                        /notif BTC 100000

                        Bot se nhac khi coin cham muc gia nay.
                        """);
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, alertService.createNotification(chatId, arguments));
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, """
                        Cach dung:
                        /notif BTC 100000

                        Bot se nhac khi coin cham muc gia nay.
                        """);
            } catch (Exception exception) {
                log.warn("Failed to create notification for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong tao duoc notification.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/daily_on")) {
            telegramMessageService.sendTextMessage(chatId, dailyMarketSummaryService.enableDailySummary(chatId));
            return;
        }

        if (isCommand(commandText, "/daily_off")) {
            telegramMessageService.sendTextMessage(chatId, dailyMarketSummaryService.disableDailySummary(chatId));
            return;
        }

        if (isCommand(commandText, "/trending")) {
            try {
                telegramMessageService.sendTextMessage(chatId, trendingCryptoService.getTopTrendingMessage());
            } catch (Exception exception) {
                log.warn("Failed to get trending crypto", exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong lay duoc trending crypto.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/tintuc")) {
            try {
                sendNewsPage(chatId, 1);
            } catch (Exception exception) {
                log.warn("Failed to get news from Telegram channel", exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong lay duoc tin tuc tu @vncointele.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/buy")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, portfolioService.getHelpMessage());
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, portfolioService.addBuy(chatId, arguments));
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, portfolioService.getHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to add buy position for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong luu duoc buy position.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/sell")) {
            String arguments = extractCommandArgument(commandText);
            if (arguments.isBlank() || cryptoPriceService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, portfolioService.getHelpMessage());
                return;
            }

            try {
                telegramMessageService.sendTextMessage(chatId, portfolioService.addSell(chatId, arguments));
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, portfolioService.getHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to add sell position for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong luu duoc sell position.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/myportfolio")) {
            telegramMessageService.sendTextMessage(chatId, portfolioService.getPortfolioMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/watch")) {
            String symbol = extractCommandArgument(commandText);
            if (symbol.isBlank() || cryptoPriceService.isHelpCommand(symbol)) {
                telegramMessageService.sendTextMessage(chatId, watchlistService.getHelpMessage());
                return;
            }

            telegramMessageService.sendTextMessage(chatId, watchlistService.addToWatchlist(chatId, symbol));
            return;
        }

        if (isCommand(commandText, "/unwatch")) {
            String symbol = extractCommandArgument(commandText);
            if (symbol.isBlank() || cryptoPriceService.isHelpCommand(symbol)) {
                telegramMessageService.sendTextMessage(chatId, watchlistService.getHelpMessage());
                return;
            }

            telegramMessageService.sendTextMessage(chatId, watchlistService.removeFromWatchlist(chatId, symbol));
            return;
        }

        if (isCommand(commandText, "/mywatchlist")) {
            sendWatchlistMessage(chatId);
            return;
        }

        if (isCommand(commandText, "/watch_updates_on")) {
            telegramMessageService.sendTextMessage(chatId, watchlistService.enableWatchUpdates(chatId));
            return;
        }

        if (isCommand(commandText, "/watch_updates_off")) {
            telegramMessageService.sendTextMessage(chatId, watchlistService.disableWatchUpdates(chatId));
            return;
        }

        if (isCommand(commandText, "/myalerts")) {
            telegramMessageService.sendTextMessage(chatId, alertService.getUserAlertsMessage(chatId));
            return;
        }

        if (isCommand(commandText, "/alert_builder")) {
            telegramMessageService.sendTextMessage(
                    chatId,
                    alertBuilderService.getStartMessage(),
                    alertBuilderService.buildSymbolKeyboard()
            );
            return;
        }

        if (isCommand(commandText, "/delete_alert")) {
            String alertId = extractCommandArgument(commandText);
            telegramMessageService.sendTextMessage(chatId, alertService.deleteAlert(chatId, alertId));
            return;
        }

        if (isCommand(commandText, "/alert")) {
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
            return;
        }

        if (isCommand(commandText, "/crypto_chart")) {
            String arguments = extractCommandArgument(commandText);
            if (cryptoChartService.isHelpCommand(arguments)) {
                telegramMessageService.sendTextMessage(chatId, cryptoChartService.getHelpMessage());
                return;
            }

            String[] parts = arguments.split("\\s+");
            if (parts.length < 2) {
                telegramMessageService.sendTextMessage(chatId, cryptoChartService.getHelpMessage());
                return;
            }

            try {
                var chartImage = cryptoChartService.getChartImage(parts[0], parts[1]);
                telegramMessageService.sendPhoto(chatId, chartImage.imageUrl(), chartImage.caption());
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, cryptoChartService.getHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to get crypto chart for arguments {}", arguments, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong tao duoc bieu do crypto.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        if (isCommand(commandText, "/crypto")) {
            String symbol = extractCommandArgument(commandText);
            if (symbol.isBlank()) {
                telegramMessageService.sendTextMessage(chatId, cryptoPriceService.getHelpMessage());
                return;
            }

            if (cryptoPriceService.isHelpCommand(symbol)) {
                telegramMessageService.sendTextMessage(chatId, cryptoPriceService.getHelpMessage());
                return;
            }

            try {
                String response = cryptoPriceService.getPriceMessage(symbol);
                telegramMessageService.sendTextMessage(chatId, response, buildCryptoKeyboard(cryptoPriceService.normalizeSymbol(symbol)));
            } catch (Exception exception) {
                log.warn("Failed to get crypto price for symbol {}", symbol, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong lay duoc gia crypto.

                        Ban thu lai sau nhe.
                        """);
            }
            return;
        }

        var naturalCommand = naturalLanguageCommandParser.parse(commandText);
        if (naturalCommand.isPresent()) {
            handleNaturalLanguageCommand(chatId, naturalCommand.get());
            return;
        }

        var naturalAlert = naturalLanguageAlertParser.parse(commandText);
        if (naturalAlert.isPresent()) {
            try {
                telegramMessageService.sendTextMessage(
                        chatId,
                        alertService.createAlert(chatId, naturalLanguageAlertParser.toAlertArguments(naturalAlert.get()))
                );
            } catch (IllegalArgumentException exception) {
                telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
            } catch (Exception exception) {
                log.warn("Failed to create natural language alert from text {}", commandText, exception);
                telegramMessageService.sendTextMessage(chatId, """
                        Tam thoi khong tao duoc alert tu cau nay.

                        Ban thu lai bang:
                        /alert BTC > 70000
                        """);
            }
            return;
        }

        telegramMessageService.sendTextMessage(chatId, """
                Minh chua hieu lenh nay.

                Thu:
                /start
                /help
                /crypto BTC
                /idea BTC
                /chart_volume BTC
                /chart_breakout BTC
                /chart_trendline BTC
                /chart_orderflow BTC
                /ai BTC
                /ai_chart BTC
                /signal BTC
                /val 1 BTC
                /notif BTC 100000
                /usdt
                /trending
                /tintuc
                /daily_on
                /daily_off
                /buy BTC 0.1 65000
                /sell BTC 61600
                /myportfolio
                /crypto_chart BTC 7d
                /watch BTC
                /mywatchlist
                /watch_updates_off
                /alert BTC > 70000
                /alert_builder
                /myalerts
                /admin_health
                /admin_metrics
                /admin_top_commands
                /admin_errors
                /admin_users
                /admin_set_plan 123456789 PRO
                /my_usage
                /my_notifications
                """);
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

    private String getMainHelpMessage() {
        return """
                Xin chao! Minh la bot theo doi thi truong.

                Lenh hien co:
                /start - xem huong dan
                /help - xem lai danh sach lenh
                /crypto BTC - xem gia crypto
                /idea BTC - tao chart idea bang Lightweight Charts
                /chart_volume BTC - xem chart Volume Delta
                /chart_breakout BTC - xem chart Breakout Confirmation
                /chart_trendline BTC - xem chart Trendline theo pivot
                /chart_orderflow BTC - xem chart Order Flow
                /ai BTC - AI quant market analysis bang GPT-5 mini
                /ai_chart BTC - ve AI Quant Map
                /signal BTC - tinh Signal Score tong hop technical + order flow
                /val 1 BTC - tinh value theo USDT va VND
                /notif BTC 100000 - nhac khi coin cham gia
                /usdt - xem gia USDT/USD theo VND P2P
                /trending - top 10 crypto dang trending
                /tintuc - xem tin moi tu @vncointele
                /daily_on - bat Daily Market Summary moi sang
                /daily_off - tat Daily Market Summary
                /buy BTC 0.1 65000 - luu lenh mua va tinh P/L
                /buy BTC 65000 - theo doi entry mua khong can so luong
                /sell BTC 61600 - theo doi entry ban
                /myportfolio - xem loi/lo portfolio
                /crypto_chart BTC 7d - xem bieu do crypto
                /watch BTC - them vao watchlist
                /unwatch BTC - xoa khoi watchlist
                /mywatchlist - xem watchlist
                /watch_updates_on - bat cap nhat watchlist
                /watch_updates_off - tat cap nhat watchlist
                /alert BTC > 70000 - tao canh bao gia
                /alert_builder - tao alert bang nut bam
                /myalerts - xem alert
                /delete_alert ALERT_ID - xoa alert
                /my_notifications - xem notification da gui gan day
                Tu nhien:
                gia btc
                ve chart eth 7 ngay
                mua btc gia 65000
                nhac toi khi sol vuot 200
                /admin_health - owner xem trang thai DB/Redis/CircuitBreaker
                /admin_metrics - owner xem metric users/alerts/subscribers
                /admin_top_commands - owner xem command duoc dung nhieu nhat
                /admin_errors - owner xem command loi gan day
                /admin_users - owner xem user activity
                /admin_set_plan CHAT_ID PRO - owner doi plan cho user
                /my_usage - xem plan va AI quota hom nay

                Sap toi minh se them:
                /stock VNM
                """;
    }

    private void handleNaturalLanguageCommand(
            Long chatId,
            NaturalLanguageCommandParser.NaturalLanguageCommand command
    ) {
        switch (command.type()) {
            case PRICE -> handleNaturalLanguagePrice(chatId, command);
            case CHART -> handleNaturalLanguageChart(chatId, command);
            case BUY -> handleNaturalLanguageBuy(chatId, command);
            case SELL -> handleNaturalLanguageSell(chatId, command);
        }
    }

    private void handleNaturalLanguagePrice(
            Long chatId,
            NaturalLanguageCommandParser.NaturalLanguageCommand command
    ) {
        try {
            String response = cryptoPriceService.getPriceMessage(command.symbol());
            telegramMessageService.sendTextMessage(chatId, response, buildCryptoKeyboard(command.symbol()));
        } catch (Exception exception) {
            log.warn("Failed to handle natural language price command {}", command, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong lay duoc gia crypto.

                    Ban thu lai bang:
                    /crypto BTC
                    """);
        }
    }

    private void handleNaturalLanguageChart(
            Long chatId,
            NaturalLanguageCommandParser.NaturalLanguageCommand command
    ) {
        try {
            var chartImage = cryptoChartService.getChartImage(command.symbol(), command.arguments());
            telegramMessageService.sendPhoto(chatId, chartImage.imageUrl(), chartImage.caption());
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, cryptoChartService.getHelpMessage());
        } catch (Exception exception) {
            log.warn("Failed to handle natural language chart command {}", command, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc bieu do crypto.

                    Ban thu lai bang:
                    /crypto_chart BTC 7d
                    """);
        }
    }

    private void handleNaturalLanguageBuy(
            Long chatId,
            NaturalLanguageCommandParser.NaturalLanguageCommand command
    ) {
        try {
            telegramMessageService.sendTextMessage(chatId, portfolioService.addBuy(chatId, command.arguments()));
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, portfolioService.getHelpMessage());
        } catch (Exception exception) {
            log.warn("Failed to handle natural language buy command {}", command, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong luu duoc buy position.

                    Ban thu lai bang:
                    /buy BTC 65000
                    """);
        }
    }

    private void handleNaturalLanguageSell(
            Long chatId,
            NaturalLanguageCommandParser.NaturalLanguageCommand command
    ) {
        try {
            telegramMessageService.sendTextMessage(chatId, portfolioService.addSell(chatId, command.arguments()));
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, portfolioService.getHelpMessage());
        } catch (Exception exception) {
            log.warn("Failed to handle natural language sell command {}", command, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong luu duoc sell position.

                    Ban thu lai bang:
                    /sell BTC 61600
                    """);
        }
    }

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
        telegramMessageService.sendTextMessage(chatId, response, buildCryptoKeyboard(symbol));
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
                buildIdeaChartKeyboard(chart.symbol(), chart.interval(), type)
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
                buildIdeaChartKeyboard(chart.symbol(), chart.interval(), "AI_CHART")
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
            telegramMessageService.sendTextMessage(
                    chatId,
                    alertBuilderService.getStartMessage(),
                    alertBuilderService.buildSymbolKeyboard()
            );
            return;
        }

        switch (parts[1]) {
            case "START" -> telegramMessageService.sendTextMessage(
                    chatId,
                    alertBuilderService.getStartMessage(),
                    alertBuilderService.buildSymbolKeyboard()
            );
            case "SYMBOL" -> handleAlertBuilderSymbolStep(chatId, parts);
            case "OPERATOR" -> handleAlertBuilderOperatorStep(chatId, parts);
            case "PERCENT" -> handleAlertBuilderPercentStep(chatId, parts);
            case "CUSTOM" -> handleAlertBuilderCustomStep(chatId, parts);
            default -> telegramMessageService.sendTextMessage(chatId, "Lua chon alert builder khong hop le.");
        }
    }

    private void handleAlertBuilderSymbolStep(Long chatId, String[] parts) {
        if (parts.length != 3 || !alertBuilderService.isSupportedSymbol(parts[2])) {
            telegramMessageService.sendTextMessage(
                    chatId,
                    alertBuilderService.getStartMessage(),
                    alertBuilderService.buildSymbolKeyboard()
            );
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
            telegramMessageService.sendTextMessage(chatId, "Chon coin:", buildMarketSymbolKeyboard());
            return;
        }

        String symbol = cryptoPriceService.normalizeSymbol(parts[1]);
        telegramMessageService.sendTextMessage(
                chatId,
                "%s menu".formatted(symbol),
                buildSymbolDashboardKeyboard(symbol)
        );
    }

    private void handleMenuCallback(Long chatId, String[] parts) {
        if (parts.length != 2) {
            telegramMessageService.sendTextMessage(chatId, getMainHelpMessage(), buildMainMenuKeyboard());
            return;
        }

        switch (parts[1]) {
            case "MAIN" -> telegramMessageService.sendTextMessage(chatId, getMainHelpMessage(), buildMainMenuKeyboard());
            case "MARKET" -> telegramMessageService.sendTextMessage(chatId, "Chon coin:", buildMarketSymbolKeyboard());
            case "CHARTS" -> telegramMessageService.sendTextMessage(chatId, "Chon coin de xem chart 7d:", buildChartSymbolKeyboard());
            case "WATCHLIST" -> sendWatchlistMessage(chatId);
            case "ALERTS" -> telegramMessageService.sendTextMessage(
                    chatId,
                    alertService.getUserAlertsMessage(chatId),
                    buildAlertMenuKeyboard()
            );
            case "PORTFOLIO" -> telegramMessageService.sendTextMessage(chatId, portfolioService.getPortfolioMessage(chatId), buildPortfolioMenuKeyboard());
            case "NEWS" -> sendNewsPage(chatId, 1);
            case "AI" -> telegramMessageService.sendTextMessage(chatId, "Chon coin de tao AI analysis:", buildAiSymbolKeyboard());
            case "SETTINGS" -> telegramMessageService.sendTextMessage(chatId, "Settings:", buildSettingsKeyboard());
            case "USAGE" -> telegramMessageService.sendTextMessage(chatId, subscriptionService.getUsageMessage(chatId), buildSettingsKeyboard());
            case "NOTIFICATIONS" -> telegramMessageService.sendTextMessage(chatId, notificationHistoryService.getMyNotificationsMessage(chatId));
            case "DAILY_ON" -> telegramMessageService.sendTextMessage(chatId, dailyMarketSummaryService.enableDailySummary(chatId), buildSettingsKeyboard());
            case "DAILY_OFF" -> telegramMessageService.sendTextMessage(chatId, dailyMarketSummaryService.disableDailySummary(chatId), buildSettingsKeyboard());
            case "WATCH_UPDATES_ON" -> telegramMessageService.sendTextMessage(chatId, watchlistService.enableWatchUpdates(chatId), buildSettingsKeyboard());
            case "WATCH_UPDATES_OFF" -> telegramMessageService.sendTextMessage(chatId, watchlistService.disableWatchUpdates(chatId), buildSettingsKeyboard());
            default -> telegramMessageService.sendTextMessage(chatId, getMainHelpMessage(), buildMainMenuKeyboard());
        }
    }

    private void sendNewsPage(Long chatId, int page) {
        NewsPage newsPage = telegramChannelNewsService.getLatestNewsPage(page);
        if (newsPage.totalPages() <= 1) {
            telegramMessageService.sendTextMessage(chatId, newsPage.message());
            return;
        }

        telegramMessageService.sendTextMessage(chatId, newsPage.message(), buildNewsKeyboard(newsPage));
    }

    private void sendWatchlistMessage(Long chatId) {
        List<String> symbols = watchlistService.getUserSymbols(chatId);
        String message = watchlistService.getWatchlistMessage(chatId);
        if (symbols.isEmpty()) {
            telegramMessageService.sendTextMessage(chatId, message, buildEmptyWatchlistKeyboard());
            return;
        }

        telegramMessageService.sendTextMessage(chatId, message, buildWatchlistKeyboard(symbols));
    }

    private InlineKeyboardMarkup buildMainMenuKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Market", "MENU:MARKET"),
                        new InlineKeyboardButton("Charts", "MENU:CHARTS")
                ),
                List.of(
                        new InlineKeyboardButton("Watchlist", "MENU:WATCHLIST"),
                        new InlineKeyboardButton("Alerts", "MENU:ALERTS")
                ),
                List.of(
                        new InlineKeyboardButton("Portfolio", "MENU:PORTFOLIO"),
                        new InlineKeyboardButton("News", "MENU:NEWS")
                ),
                List.of(
                        new InlineKeyboardButton("AI", "MENU:AI"),
                        new InlineKeyboardButton("Settings", "MENU:SETTINGS")
                )
        ));
    }

    private InlineKeyboardMarkup buildMarketSymbolKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("BTC", "MARKET_SYMBOL:BTC"),
                        new InlineKeyboardButton("ETH", "MARKET_SYMBOL:ETH"),
                        new InlineKeyboardButton("SOL", "MARKET_SYMBOL:SOL")
                ),
                List.of(
                        new InlineKeyboardButton("BNB", "MARKET_SYMBOL:BNB"),
                        new InlineKeyboardButton("XRP", "MARKET_SYMBOL:XRP"),
                        new InlineKeyboardButton("DOGE", "MARKET_SYMBOL:DOGE")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    private InlineKeyboardMarkup buildChartSymbolKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("BTC 7d", "CHART:BTC:7d"),
                        new InlineKeyboardButton("ETH 7d", "CHART:ETH:7d"),
                        new InlineKeyboardButton("SOL 7d", "CHART:SOL:7d")
                ),
                List.of(
                        new InlineKeyboardButton("BNB 7d", "CHART:BNB:7d"),
                        new InlineKeyboardButton("XRP 7d", "CHART:XRP:7d"),
                        new InlineKeyboardButton("DOGE 7d", "CHART:DOGE:7d")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    private InlineKeyboardMarkup buildAiSymbolKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("BTC AI", "AI_PREDICTION:BTC:4h"),
                        new InlineKeyboardButton("ETH AI", "AI_PREDICTION:ETH:4h"),
                        new InlineKeyboardButton("SOL AI", "AI_PREDICTION:SOL:4h")
                ),
                List.of(
                        new InlineKeyboardButton("AI Chart BTC", "AI_CHART:BTC:4h"),
                        new InlineKeyboardButton("AI Chart ETH", "AI_CHART:ETH:4h")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    private InlineKeyboardMarkup buildSymbolDashboardKeyboard(String symbol) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Price", "PRICE:" + symbol),
                        new InlineKeyboardButton("Chart 7d", "CHART:" + symbol + ":7d")
                ),
                List.of(
                        new InlineKeyboardButton("AI", "AI_PREDICTION:" + symbol + ":4h"),
                        new InlineKeyboardButton("AI Chart", "AI_CHART:" + symbol + ":4h")
                ),
                List.of(
                        new InlineKeyboardButton("Signal", "SIGNAL:" + symbol),
                        new InlineKeyboardButton("Watch", "WATCH:" + symbol)
                ),
                List.of(
                        new InlineKeyboardButton("Alert", "ALERT_PROMPT:" + symbol),
                        new InlineKeyboardButton("Main Menu", "MENU:MAIN")
                )
        ));
    }

    private InlineKeyboardMarkup buildWatchlistKeyboard(List<String> symbols) {
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        for (String symbol : symbols) {
            rows.add(List.of(
                    new InlineKeyboardButton(symbol + " Price", "PRICE:" + symbol),
                    new InlineKeyboardButton("Chart", "CHART:" + symbol + ":7d"),
                    new InlineKeyboardButton("AI", "AI_PREDICTION:" + symbol + ":4h")
            ));
            rows.add(List.of(
                    new InlineKeyboardButton("Signal", "SIGNAL:" + symbol),
                    new InlineKeyboardButton("Unwatch " + symbol, "UNWATCH:" + symbol)
            ));
        }
        rows.add(List.of(
                new InlineKeyboardButton("Alert Builder", "MENU:ALERTS"),
                new InlineKeyboardButton("Main Menu", "MENU:MAIN")
        ));

        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardMarkup buildEmptyWatchlistKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Watch BTC", "WATCH:BTC"),
                        new InlineKeyboardButton("Watch ETH", "WATCH:ETH"),
                        new InlineKeyboardButton("Watch SOL", "WATCH:SOL")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    private InlineKeyboardMarkup buildAlertMenuKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Alert Builder", "ALERT_BUILDER:START"),
                        new InlineKeyboardButton("BTC Alert", "ALERT_PROMPT:BTC")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    private InlineKeyboardMarkup buildPortfolioMenuKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("BTC Price", "PRICE:BTC"),
                        new InlineKeyboardButton("BTC Chart", "CHART:BTC:7d")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    private InlineKeyboardMarkup buildSettingsKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("My Usage", "MENU:USAGE"),
                        new InlineKeyboardButton("Notifications", "MENU:NOTIFICATIONS")
                ),
                List.of(
                        new InlineKeyboardButton("Daily On", "MENU:DAILY_ON"),
                        new InlineKeyboardButton("Daily Off", "MENU:DAILY_OFF")
                ),
                List.of(
                        new InlineKeyboardButton("Watch Updates On", "MENU:WATCH_UPDATES_ON"),
                        new InlineKeyboardButton("Watch Updates Off", "MENU:WATCH_UPDATES_OFF")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    private InlineKeyboardMarkup buildCryptoKeyboard(String symbol) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Chart 7d", "CHART:" + symbol + ":7d"),
                        new InlineKeyboardButton("Chart 1m", "CHART:" + symbol + ":1m")
                ),
                List.of(
                        new InlineKeyboardButton("Add Watchlist", "WATCH:" + symbol),
                        new InlineKeyboardButton("Create Alert", "ALERT_PROMPT:" + symbol)
                ),
                List.of(
                        new InlineKeyboardButton("AI", "AI_PREDICTION:" + symbol + ":4h"),
                        new InlineKeyboardButton("Signal", "SIGNAL:" + symbol)
                )
        ));
    }

    private InlineKeyboardMarkup buildIdeaChartKeyboard(String symbol, String interval, String currentType) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton(
                                currentType.equals("IDEA") ? "Idea Chart" : "Idea",
                                "IDEA_CHART:IDEA:" + symbol + ":" + interval
                        ),
                        new InlineKeyboardButton(
                                currentType.equals("VOLUME") ? "Volume Delta" : "Volume",
                                "IDEA_CHART:VOLUME:" + symbol + ":" + interval
                        )
                ),
                List.of(
                        new InlineKeyboardButton(
                                currentType.equals("BREAKOUT") ? "Breakout Chart" : "Breakout",
                                "IDEA_CHART:BREAKOUT:" + symbol + ":" + interval
                        ),
                        new InlineKeyboardButton(
                                currentType.equals("TRENDLINE") ? "Trendline Chart" : "Trendline",
                                "IDEA_CHART:TRENDLINE:" + symbol + ":" + interval
                        )
                ),
                List.of(
                        new InlineKeyboardButton(
                                currentType.equals("ORDER_FLOW") ? "Order Flow Chart" : "Order Flow",
                                "IDEA_CHART:ORDER_FLOW:" + symbol + ":" + interval
                        )
                ),
                List.of(
                        new InlineKeyboardButton(
                                "AI",
                                "AI_PREDICTION:" + symbol + ":" + interval
                        ),
                        new InlineKeyboardButton(
                                currentType.equals("AI_CHART") ? "AI Chart" : "AI Chart",
                                "AI_CHART:" + symbol + ":" + interval
                        )
                )
        ));
    }

    private InlineKeyboardMarkup buildNewsKeyboard(NewsPage newsPage) {
        if (newsPage.page() <= 1) {
            return new InlineKeyboardMarkup(List.of(
                    List.of(new InlineKeyboardButton("Next", "NEWS_PAGE:" + (newsPage.page() + 1)))
            ));
        }

        if (newsPage.page() >= newsPage.totalPages()) {
            return new InlineKeyboardMarkup(List.of(
                    List.of(new InlineKeyboardButton("Before", "NEWS_PAGE:" + (newsPage.page() - 1)))
            ));
        }

        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Before", "NEWS_PAGE:" + (newsPage.page() - 1)),
                        new InlineKeyboardButton("Next", "NEWS_PAGE:" + (newsPage.page() + 1))
                )
        ));
    }

    private boolean isCommand(String commandText, String command) {
        return commandText.equalsIgnoreCase(command)
                || commandText.toLowerCase().startsWith(command.toLowerCase() + " ");
    }

    private String extractCommandArgument(String commandText) {
        String[] parts = commandText.split("\\s+", 2);
        if (parts.length < 2) {
            return "";
        }

        return parts[1];
    }

    private String extractCommandName(String commandText) {
        if (commandText == null || commandText.isBlank()) {
            return "UNKNOWN";
        }

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
