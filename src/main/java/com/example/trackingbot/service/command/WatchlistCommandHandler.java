package com.example.trackingbot.service.command;

import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.telegram.TelegramCallbackService;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import com.example.trackingbot.service.watchlist.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(90)
@RequiredArgsConstructor
public class WatchlistCommandHandler implements CommandHandler {

    private final TelegramMessageService telegramMessageService;
    private final TelegramCallbackService telegramCallbackService;
    private final CryptoPriceService cryptoPriceService;
    private final WatchlistService watchlistService;

    @Override
    public CommandHandleResult handle(Long chatId, String commandText) {
        if (isCommand(commandText, "/watch")) {
            String symbol = extractCommandArgument(commandText);
            if (symbol.isBlank() || cryptoPriceService.isHelpCommand(symbol)) {
                telegramMessageService.sendTextMessage(chatId, watchlistService.getHelpMessage());
                return CommandHandleResult.handledSuccessfully();
            }

            telegramMessageService.sendTextMessage(chatId, watchlistService.addToWatchlist(chatId, symbol));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/unwatch")) {
            String symbol = extractCommandArgument(commandText);
            if (symbol.isBlank() || cryptoPriceService.isHelpCommand(symbol)) {
                telegramMessageService.sendTextMessage(chatId, watchlistService.getHelpMessage());
                return CommandHandleResult.handledSuccessfully();
            }

            telegramMessageService.sendTextMessage(chatId, watchlistService.removeFromWatchlist(chatId, symbol));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/mywatchlist")) {
            telegramCallbackService.sendWatchlistMessage(chatId);
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/watch_updates_on")) {
            telegramMessageService.sendTextMessage(chatId, watchlistService.enableWatchUpdates(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        if (isCommand(commandText, "/watch_updates_off")) {
            telegramMessageService.sendTextMessage(chatId, watchlistService.disableWatchUpdates(chatId));
            return CommandHandleResult.handledSuccessfully();
        }

        return CommandHandleResult.notHandled();
    }
}
