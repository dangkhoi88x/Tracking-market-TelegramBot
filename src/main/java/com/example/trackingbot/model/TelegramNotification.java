package com.example.trackingbot.model;

import java.time.Instant;

public record TelegramNotification(String id,
                                   Long chatId,
                                   String type,
                                   String text,
                                   Instant createdAt) {
}
