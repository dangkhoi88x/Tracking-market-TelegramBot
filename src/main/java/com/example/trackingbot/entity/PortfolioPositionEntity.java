package com.example.trackingbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "portfolio_positions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioPositionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private TelegramUser user;

    @Column(nullable = false, length = 10)
    private String side;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(precision = 24, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal entryPrice;

    private Instant createdAt;

    private Instant updatedAt;

    public PortfolioPositionEntity(String id, TelegramUser user, String side, String symbol, BigDecimal amount, BigDecimal entryPrice) {
        this.id = id;
        this.user = user;
        this.side = side;
        this.symbol = symbol;
        this.amount = amount;
        this.entryPrice = entryPrice;
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
