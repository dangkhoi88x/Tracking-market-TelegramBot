package com.example.trackingbot.model;

public enum SubscriptionPlan {
    FREE(5),
    PRO(50),
    ADMIN(Integer.MAX_VALUE);

    private final int dailyAiQuota;

    SubscriptionPlan(int dailyAiQuota) {
        this.dailyAiQuota = dailyAiQuota;
    }

    public int dailyAiQuota() {
        return dailyAiQuota;
    }

    public boolean unlimited() {
        return this == ADMIN;
    }
}
