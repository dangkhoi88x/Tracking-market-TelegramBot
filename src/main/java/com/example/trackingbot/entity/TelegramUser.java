package com.example.trackingbot.entity;

import com.example.trackingbot.model.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "telegram_users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    private String username;

    private String firstName;

    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    private Instant createdAt;

    private Instant updatedAt;

    public TelegramUser(Long chatId) {
        this.chatId = chatId;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
