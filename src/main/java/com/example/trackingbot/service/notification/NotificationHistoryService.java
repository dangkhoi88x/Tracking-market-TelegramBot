package com.example.trackingbot.service.notification;

import com.example.trackingbot.entity.NotificationHistoryEntity;
import com.example.trackingbot.model.TelegramNotification;
import com.example.trackingbot.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private static final int HISTORY_LIMIT = 10;
    private static final int PREVIEW_MAX_LENGTH = 140;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd-MM HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final NotificationHistoryRepository notificationHistoryRepository;

    @Transactional
    public void recordSent(TelegramNotification notification) {
        if (notificationHistoryRepository.existsByNotificationId(notification.id())) {
            return;
        }

        try {
            notificationHistoryRepository.save(new NotificationHistoryEntity(
                    notification.id(),
                    notification.chatId(),
                    notification.type(),
                    notification.text(),
                    Instant.now()
            ));
        } catch (DataIntegrityViolationException ignored) {
            // Retry delivery can race with an already-recorded notification id.
        }
    }

    @Transactional(readOnly = true)
    public String getMyNotificationsMessage(Long chatId) {
        List<NotificationHistoryEntity> notifications = notificationHistoryRepository.findByChatIdOrderBySentAtDesc(
                chatId,
                PageRequest.of(0, HISTORY_LIMIT)
        );

        if (notifications.isEmpty()) {
            return """
                    Ban chua co notification nao.

                    Thu tao alert hoac bat watchlist updates:
                    /alert BTC > 70000
                    /watch BTC
                    /watch_updates_on
                    """;
        }

        StringBuilder message = new StringBuilder("Notification gan day:\n\n");
        for (int index = 0; index < notifications.size(); index++) {
            NotificationHistoryEntity notification = notifications.get(index);
            message.append("%d. %s | %s%n%s%n%n".formatted(
                    index + 1,
                    TIME_FORMATTER.format(notification.getSentAt()),
                    formatType(notification),
                    preview(notification.getText())
            ));
        }

        return message.toString().trim();
    }

    private String formatType(NotificationHistoryEntity notification) {
        return switch (notification.getType()) {
            case ALERT_TRIGGERED -> "Alert";
            case WATCHLIST_UPDATE -> "Watchlist";
            case DAILY_SUMMARY -> "Daily";
            case GENERAL -> "General";
        };
    }

    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "(empty)";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= PREVIEW_MAX_LENGTH
                ? normalized
                : normalized.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }
}
