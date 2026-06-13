package com.example.trackingbot.entity;

import com.example.trackingbot.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36, unique = true)
    private String notificationId;

    @Column(nullable = false)
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private Instant sentAt;

    @Column(nullable = false)
    private Instant createdAt;

    public NotificationHistoryEntity(
            String notificationId,
            Long chatId,
            NotificationType type,
            String text,
            Instant sentAt
    ) {
        this.notificationId = notificationId;
        this.chatId = chatId;
        this.type = type;
        this.text = text;
        this.sentAt = sentAt;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
