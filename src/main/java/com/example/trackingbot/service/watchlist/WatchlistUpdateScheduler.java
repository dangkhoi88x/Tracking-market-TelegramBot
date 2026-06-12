package com.example.trackingbot.service.watchlist;

import com.example.trackingbot.model.UserWatchlist;
import com.example.trackingbot.model.NotificationType;
import com.example.trackingbot.service.notification.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchlistUpdateScheduler {

    private static final Logger log = LoggerFactory.getLogger(WatchlistUpdateScheduler.class);

    private final WatchlistService watchlistService;
    private final NotificationPublisher notificationPublisher;

    @Scheduled(fixedRate = 300_000, initialDelay = 300_000)
    public void sendWatchlistUpdates() {
        for (UserWatchlist watchlist : watchlistService.getAllWatchlists()) {
            if (!watchlistService.isWatchUpdatesEnabled(watchlist.chatId())) {
                continue;
            }

            try {
               // rabbitMQ
                notificationPublisher.publishTelegramNotification(
                        watchlist.chatId(),
                        NotificationType.WATCHLIST_UPDATE,
                        watchlistService.buildWatchlistUpdateMessage(watchlist)
                );



            } catch (Exception exception) {
                log.warn("Failed to send watchlist update to chat {}", watchlist.chatId(), exception);
            }
        }
    }
}
