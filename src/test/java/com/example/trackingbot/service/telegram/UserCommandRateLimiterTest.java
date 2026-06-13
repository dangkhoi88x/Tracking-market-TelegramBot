package com.example.trackingbot.service.telegram;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserCommandRateLimiterTest {

    @Test
    void checkAllowed_shouldLimitUserToTwentyCommandsPerMinute() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(new InMemoryRateLimitStore(), clock);
        Long chatId = 123L;

        for (int index = 0; index < 20; index++) {
            assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isTrue();
        }

        UserCommandRateLimiter.RateLimitResult result = limiter.checkAllowed(chatId, "/crypto ETH");

        assertThat(result.allowed()).isFalse();
        assertThat(result.ruleName()).isEqualTo("Tat ca lenh");
        assertThat(result.maxRequests()).isEqualTo(20);
        assertThat(result.windowLabel()).isEqualTo("phut");
        assertThat(result.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void checkAllowed_shouldLimitAiCommandToThreeRequestsPerDay() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(new InMemoryRateLimitStore(), clock);
        Long chatId = 123L;

        assertThat(limiter.checkAllowed(chatId, "/ai BTC").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai ETH").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai SOL").allowed()).isTrue();

        UserCommandRateLimiter.RateLimitResult result = limiter.checkAllowed(chatId, "/ai BNB");

        assertThat(result.allowed()).isFalse();
        assertThat(result.ruleName()).isEqualTo("AI analysis");
        assertThat(result.maxRequests()).isEqualTo(3);
        assertThat(result.windowLabel()).isEqualTo("ngay");
    }

    @Test
    void checkAllowed_shouldLimitAiChartCommandToTwoRequestsPerDay() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(new InMemoryRateLimitStore(), clock);
        Long chatId = 123L;

        assertThat(limiter.checkAllowed(chatId, "/ai_chart BTC").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai_chart ETH").allowed()).isTrue();

        UserCommandRateLimiter.RateLimitResult result = limiter.checkAllowed(chatId, "/ai_chart SOL");

        assertThat(result.allowed()).isFalse();
        assertThat(result.ruleName()).isEqualTo("AI chart");
        assertThat(result.maxRequests()).isEqualTo(2);
        assertThat(result.windowLabel()).isEqualTo("ngay");
    }

    @Test
    void checkAllowed_shouldAllowCommandsAgainAfterWindowExpires() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(new InMemoryRateLimitStore(), clock);
        Long chatId = 123L;

        for (int index = 0; index < 20; index++) {
            assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isTrue();
        }
        assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(61));

        assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isTrue();
    }

    @Test
    void checkAllowed_shouldNotConsumeAiQuotaWhenGlobalRuleRejects() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(new InMemoryRateLimitStore(), clock);
        Long chatId = 123L;

        for (int index = 0; index < 20; index++) {
            assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isTrue();
        }

        assertThat(limiter.checkAllowed(chatId, "/ai BTC").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(61));

        assertThat(limiter.checkAllowed(chatId, "/ai BTC").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai ETH").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai SOL").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai BNB").allowed()).isFalse();
    }

    private static class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-06-12T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }

    private static class InMemoryRateLimitStore implements RateLimitStore {

        private final Map<String, ArrayDeque<Instant>> timestampsByKey = new HashMap<>();

        @Override
        public RateLimitStoreResult checkAndIncrement(List<RateLimitStoreRequest> requests) {
            for (RateLimitStoreRequest request : requests) {
                ArrayDeque<Instant> timestamps = timestampsByKey.computeIfAbsent(request.key(), ignored -> new ArrayDeque<>());
                removeExpired(timestamps, request.now(), request.window());

                if (timestamps.size() >= request.maxRequests()) {
                    return RateLimitStoreResult.reject(
                            requests.indexOf(request),
                            calculateRetryAfterSeconds(timestamps, request.now(), request.window())
                    );
                }
            }

            for (RateLimitStoreRequest request : requests) {
                timestampsByKey.get(request.key()).addLast(request.now());
            }

            return RateLimitStoreResult.allow();
        }

        private void removeExpired(ArrayDeque<Instant> timestamps, Instant now, Duration window) {
            Instant cutoff = now.minus(window);
            while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
                timestamps.removeFirst();
            }
        }

        private long calculateRetryAfterSeconds(ArrayDeque<Instant> timestamps, Instant now, Duration window) {
            Instant retryAt = timestamps.peekFirst().plus(window);
            long retryAfterMillis = Duration.between(now, retryAt).toMillis();
            if (retryAfterMillis <= 0) {
                return 1;
            }

            return Math.max(1, (retryAfterMillis + 999) / 1000);
        }
    }
}
