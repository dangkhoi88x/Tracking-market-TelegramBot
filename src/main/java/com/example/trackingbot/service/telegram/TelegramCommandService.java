package com.example.trackingbot.service.telegram;

import com.example.trackingbot.dto.request.InlineKeyboardButton;
import com.example.trackingbot.dto.request.InlineKeyboardMarkup;
import com.example.trackingbot.service.alert.AlertService;
import com.example.trackingbot.service.analysis.AiPredictionService;
import com.example.trackingbot.service.chart.IdeaChartService;
import com.example.trackingbot.service.crypto.CryptoChartService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.crypto.TrendingCryptoService;
import com.example.trackingbot.service.crypto.UsdtRateService;
import com.example.trackingbot.service.crypto.ValueConversionService;
import com.example.trackingbot.service.daily.DailyMarketSummaryService;
import com.example.trackingbot.service.portfolio.PortfolioService;
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
    private final TrendingCryptoService trendingCryptoService;
    private final PortfolioService portfolioService;
    private final DailyMarketSummaryService dailyMarketSummaryService;
    private final ValueConversionService valueConversionService;
    private final UsdtRateService usdtRateService;
    private final IdeaChartService ideaChartService;
    private final AiPredictionService aiPredictionService;

    public void handleTextMessage(Long chatId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String commandText = text.trim();

        if ("/start".equalsIgnoreCase(commandText) || "/help".equalsIgnoreCase(commandText)) {
            telegramMessageService.sendTextMessage(chatId, getMainHelpMessage());
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
            telegramMessageService.sendTextMessage(chatId, watchlistService.getWatchlistMessage(chatId));
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
                /val 1 BTC
                /notif BTC 100000
                /usdt
                /trending
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
                /myalerts
                """);
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
                /val 1 BTC - tinh value theo USDT va VND
                /notif BTC 100000 - nhac khi coin cham gia
                /usdt - xem gia USDT/USD theo VND P2P
                /trending - top 10 crypto dang trending
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
                /myalerts - xem alert
                /delete_alert ALERT_ID - xoa alert

                Sap toi minh se them:
                /stock VNM
                """;
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
                case "WATCH" -> handleWatchCallback(chatId, parts);
                case "ALERT_PROMPT" -> handleAlertPromptCallback(chatId, parts);
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

        String arguments = parts[1] + " " + parts[2];
        telegramMessageService.sendTextMessage(chatId, "Dang tao AI quant analysis bang GPT-5 mini, doi minh mot chut...");
        telegramMessageService.sendTextMessage(chatId, aiPredictionService.getAiPredictionMessage(arguments));
    }

    private void handleAiChartCallback(Long chatId, String[] parts) {
        if (parts.length != 3) {
            telegramMessageService.sendTextMessage(chatId, aiPredictionService.getHelpMessage());
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

    private InlineKeyboardMarkup buildCryptoKeyboard(String symbol) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Chart 7d", "CHART:" + symbol + ":7d"),
                        new InlineKeyboardButton("Chart 1m", "CHART:" + symbol + ":1m")
                ),
                List.of(
                        new InlineKeyboardButton("Add Watchlist", "WATCH:" + symbol),
                        new InlineKeyboardButton("Create Alert", "ALERT_PROMPT:" + symbol)
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
}
