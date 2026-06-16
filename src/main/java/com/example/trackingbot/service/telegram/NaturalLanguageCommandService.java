package com.example.trackingbot.service.telegram;

import com.example.trackingbot.service.alert.AlertService;
import com.example.trackingbot.service.alert.NaturalLanguageAlertParser;
import com.example.trackingbot.service.crypto.CryptoChartService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.portfolio.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class    NaturalLanguageCommandService {

    private static final Logger log = LoggerFactory.getLogger(NaturalLanguageCommandService.class);

    private final TelegramMessageService telegramMessageService;
    private final TelegramKeyboardService telegramKeyboardService;
    private final NaturalLanguageCommandParser naturalLanguageCommandParser;
    private final NaturalLanguageAlertParser naturalLanguageAlertParser;
    private final CryptoPriceService cryptoPriceService;
    private final CryptoChartService cryptoChartService;
    private final PortfolioService portfolioService;
    private final AlertService alertService;

    public boolean handle(Long chatId, String text) {
        return handleMarketCommand(chatId, text) || handleAlertCommand(chatId, text);
    }

    private boolean handleMarketCommand(Long chatId, String text) {
        var naturalCommand = naturalLanguageCommandParser.parse(text);
        if (naturalCommand.isEmpty()) {
            return false;
        }

        switch (naturalCommand.get().type()) {
            case PRICE -> handlePrice(chatId, naturalCommand.get());
            case CHART -> handleChart(chatId, naturalCommand.get());
            case BUY -> handleBuy(chatId, naturalCommand.get());
            case SELL -> handleSell(chatId, naturalCommand.get());
        }

        return true;
    }

    private boolean handleAlertCommand(Long chatId, String text) {
        var naturalAlert = naturalLanguageAlertParser.parse(text);
        if (naturalAlert.isEmpty()) {
            return false;
        }

        try {
            telegramMessageService.sendTextMessage(
                    chatId,
                    alertService.createAlert(chatId, naturalLanguageAlertParser.toAlertArguments(naturalAlert.get()))
            );
        } catch (IllegalArgumentException exception) {
            telegramMessageService.sendTextMessage(chatId, alertService.getHelpMessage());
        } catch (Exception exception) {
            log.warn("Failed to create natural language alert from text {}", text, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong tao duoc alert tu cau nay.

                    Ban thu lai bang:
                    /alert BTC > 70000
                    """);
        }

        return true;
    }

    private void handlePrice(Long chatId, NaturalLanguageCommandParser.NaturalLanguageCommand command) {
        try {
            String response = cryptoPriceService.getPriceMessage(command.symbol());
            telegramMessageService.sendTextMessage(
                    chatId,
                    response,
                    telegramKeyboardService.buildCryptoKeyboard(command.symbol())
            );
        } catch (Exception exception) {
            log.warn("Failed to handle natural language price command {}", command, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong lay duoc gia crypto.

                    Ban thu lai bang:
                    /crypto BTC
                    """);
        }
    }

    private void handleChart(Long chatId, NaturalLanguageCommandParser.NaturalLanguageCommand command) {
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

    private void handleBuy(Long chatId, NaturalLanguageCommandParser.NaturalLanguageCommand command) {
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

    private void handleSell(Long chatId, NaturalLanguageCommandParser.NaturalLanguageCommand command) {
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
}
