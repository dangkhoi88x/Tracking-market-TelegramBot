package com.example.trackingbot.service.command;

import com.example.trackingbot.service.crypto.CryptoChartService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.crypto.TrendingCryptoService;
import com.example.trackingbot.service.crypto.UsdtRateService;
import com.example.trackingbot.service.crypto.ValueConversionService;
import com.example.trackingbot.service.telegram.TelegramKeyboardService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
@RequiredArgsConstructor
public class MarketCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(MarketCommandHandler.class);

    private final TelegramMessageService telegramMessageService;
    private final TelegramKeyboardService telegramKeyboardService;
    private final CryptoPriceService cryptoPriceService;
    private final CryptoChartService cryptoChartService;
    private final TrendingCryptoService trendingCryptoService;
    private final ValueConversionService valueConversionService;
    private final UsdtRateService usdtRateService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/usdt")) {
            handleUsdt(chatId);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/val")) {
            handleValue(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/trending")) {
            handleTrending(chatId);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/crypto_chart")) {
            handleCryptoChart(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/crypto")) {
            handleCrypto(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }

    private void handleUsdt(Long chatId) {
        try {
            telegramMessageService.sendTextMessage(chatId, usdtRateService.getUsdtMessage());
        } catch (Exception exception) {
            log.warn("Failed to get USDT/VND rate", exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong lay duoc gia USDT/VND.

                    Ban thu lai sau nhe.
                    """);
        }
    }

    private void handleValue(Long chatId, String commandText) {
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
    }

    private void handleTrending(Long chatId) {
        try {
            telegramMessageService.sendTextMessage(chatId, trendingCryptoService.getTopTrendingMessage());
        } catch (Exception exception) {
            log.warn("Failed to get trending crypto", exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong lay duoc trending crypto.

                    Ban thu lai sau nhe.
                    """);
        }
    }

    private void handleCryptoChart(Long chatId, String commandText) {
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
    }

    private void handleCrypto(Long chatId, String commandText) {
        String symbol = extractCommandArgument(commandText);
        if (symbol.isBlank() || cryptoPriceService.isHelpCommand(symbol)) {
            telegramMessageService.sendTextMessage(chatId, cryptoPriceService.getHelpMessage());
            return;
        }

        try {
            String response = cryptoPriceService.getPriceMessage(symbol);
            telegramMessageService.sendTextMessage(
                    chatId,
                    response,
                    telegramKeyboardService.buildCryptoKeyboard(cryptoPriceService.normalizeSymbol(symbol))
            );
        } catch (Exception exception) {
            log.warn("Failed to get crypto price for symbol {}", symbol, exception);
            telegramMessageService.sendTextMessage(chatId, """
                    Tam thoi khong lay duoc gia crypto.

                    Ban thu lai sau nhe.
                    """);
        }
    }
}
