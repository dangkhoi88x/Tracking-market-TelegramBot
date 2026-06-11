package com.example.trackingbot.service.watchlist;

import com.example.trackingbot.dto.entity.UserWatchlist;
import com.example.trackingbot.service.telegram.TelegramMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WatchlistUpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(WatchlistUpdateScheduler.class);

    private final WatchlistService watchlistService;
    private final TelegramMessageService telegramMessageService;

    public WatchlistUpdateScheduler(
            WatchlistService watchlistService,
            TelegramMessageService telegramMessageService
    ) {
        this.watchlistService = watchlistService;
        this.telegramMessageService = telegramMessageService;
    }

    @Scheduled(fixedRate = 300_000, initialDelay = 300_000)
    public void sendWatchlistUpdates() {
        for (UserWatchlist watchlist : watchlistService.getAllWatchlists()) {
            if (!watchlistService.isWatchUpdatesEnabled(watchlist.chatId())) {
                continue;
            }

            try {
                telegramMessageService.sendTextMessage(
                        watchlist.chatId(),
                        watchlistService.buildWatchlistUpdateMessage(watchlist)
                );
            } catch (Exception exception) {
                log.warn("Failed to send watchlist update to chat {}", watchlist.chatId(), exception);
            }
        }
    }
}
