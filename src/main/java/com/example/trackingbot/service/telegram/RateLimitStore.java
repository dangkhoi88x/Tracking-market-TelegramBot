package com.example.trackingbot.service.telegram;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface RateLimitStore {

    RateLimitStoreResult checkAndIncrement(List<RateLimitStoreRequest> requests);

    record RateLimitStoreRequest(
            String key,
            int maxRequests,
            Duration window,
            Instant now
    ) {
    }

    record RateLimitStoreResult(
            boolean allowed,
            int rejectedIndex,
            long retryAfterSeconds
    ) {

        public static RateLimitStoreResult allow() {
            return new RateLimitStoreResult(true, -1, 0);
        }

        public static RateLimitStoreResult reject(int rejectedIndex, long retryAfterSeconds) {
            return new RateLimitStoreResult(false, rejectedIndex, retryAfterSeconds);
        }
    }
}
