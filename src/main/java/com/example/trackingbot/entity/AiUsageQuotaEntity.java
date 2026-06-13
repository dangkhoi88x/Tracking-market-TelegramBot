package com.example.trackingbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "ai_usage_quotas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_usage_user_feature_date",
                columnNames = {"user_id", "feature", "usage_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiUsageQuotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private TelegramUser user;

    @Column(nullable = false, length = 50)
    private String feature;

    @Column(nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private int usedCount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public AiUsageQuotaEntity(TelegramUser user, String feature, LocalDate usageDate) {
        this.user = user;
        this.feature = feature;
        this.usageDate = usageDate;
    }

    public void increase() {
        usedCount++;
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
