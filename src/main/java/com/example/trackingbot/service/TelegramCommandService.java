package com.example.trackingbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramCommandService {

    private static final Logger log = LoggerFactory.getLogger(TelegramCommandService.class);

    private final TelegramMessageService telegramMessageService;
    private final CryptoPriceService cryptoPriceService;
    private final CryptoChartService cryptoChartService;
    private final WatchlistService watchlistService;
    private final AlertService alertService;

    public TelegramCommandService(
            TelegramMessageService telegramMessageService,
            CryptoPriceService cryptoPriceService,
            CryptoChartService cryptoChartService,
            WatchlistService watchlistService,
            AlertService alertService
    ) {
        this.telegramMessageService = telegramMessageService;
        this.cryptoPriceService = cryptoPriceService;
        this.cryptoChartService = cryptoChartService;
        this.watchlistService = watchlistService;
        this.alertService = alertService;
    }

    public void handleTextMessage(Long chatId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String commandText = text.trim();

        if ("/start".equalsIgnoreCase(commandText) || "/help".equalsIgnoreCase(commandText)) {
            telegramMessageService.sendTextMessage(chatId, getMainHelpMessage());
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
                telegramMessageService.sendTextMessage(chatId, response);
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
