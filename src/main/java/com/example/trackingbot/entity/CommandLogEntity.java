package com.example.trackingbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "command_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommandLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false, length = 100)
    private String command;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private long durationMs;

    @Column(nullable = false)
    private Instant createdAt;

    public CommandLogEntity(Long chatId, String command, boolean success, String errorMessage, long durationMs) {
        this.chatId = chatId;
        this.command = command;
        this.success = success;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
