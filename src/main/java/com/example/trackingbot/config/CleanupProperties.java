package com.example.trackingbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cleanup")
public record CleanupProperties(
        Boolean enabled,
        Duration inactiveAlertRetention,
        Duration chartFileRetention
) {
    public boolean enabledOrDefault() {
        return enabled == null || enabled;
    }

    public Duration inactiveAlertRetentionOrDefault() {
        return inactiveAlertRetention == null ? Duration.ofDays(30) : inactiveAlertRetention;
    }

    public Duration chartFileRetentionOrDefault() {
        return chartFileRetention == null ? Duration.ofDays(1) : chartFileRetention;
    }
}
