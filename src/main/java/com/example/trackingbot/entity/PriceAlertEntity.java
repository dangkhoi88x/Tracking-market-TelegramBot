package com.example.trackingbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_alerts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceAlertEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private TelegramUser user;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(nullable = false, length = 2)
    private String operator;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal targetPrice;

    @Column(nullable = false)
    private boolean active;

    private Instant createdAt;

    private Instant triggeredAt;

    private Instant deletedAt;

    public PriceAlertEntity(String id, TelegramUser user, String symbol, String operator, BigDecimal targetPrice) {
        this.id = id;
        this.user = user;
        this.symbol = symbol;
        this.operator = operator;
        this.targetPrice = targetPrice;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void markTriggered() {
        active = false;
        triggeredAt = Instant.now();
    }

    public void markDeleted() {
        active = false;
        deletedAt = Instant.now();
    }
}
