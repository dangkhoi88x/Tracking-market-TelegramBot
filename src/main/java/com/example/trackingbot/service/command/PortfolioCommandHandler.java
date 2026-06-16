package com.example.trackingbot.service.command;

import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.portfolio.PortfolioService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(80)
@RequiredArgsConstructor
public class PortfolioCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(PortfolioCommandHandler.class);

    private final TelegramMessageService telegramMessageService;
    private final CryptoPriceService cryptoPriceService;
    private final PortfolioService portfolioService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/buy")) {
            handleBuy(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/sell")) {
            handleSell(chatId, commandText);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/myportfolio")) {
            telegramMessageService.sendTextMessage(chatId, portfolioService.getPortfolioMessage(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }

    private void handleBuy(Long chatId, String commandText) {
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
    }

    private void handleSell(Long chatId, String commandText) {
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
    }
}
