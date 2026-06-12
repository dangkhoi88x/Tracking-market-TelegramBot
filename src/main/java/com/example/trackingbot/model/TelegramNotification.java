package com.example.trackingbot.model;

import java.time.Instant;

public record TelegramNotification(String id,
                                   Long chatId,
                                   NotificationType type,
                                   String text,
                                   Instant createdAt) {
}
