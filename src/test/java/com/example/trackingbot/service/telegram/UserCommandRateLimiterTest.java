package com.example.trackingbot.service.telegram;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class UserCommandRateLimiterTest {

    @Test
    void checkAllowed_shouldLimitUserToTenCommandsPerMinute() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(clock);
        Long chatId = 123L;

        for (int index = 0; index < 10; index++) {
            assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isTrue();
        }

        UserCommandRateLimiter.RateLimitResult result = limiter.checkAllowed(chatId, "/crypto ETH");

        assertThat(result.allowed()).isFalse();
        assertThat(result.ruleName()).isEqualTo("Tat ca lenh");
        assertThat(result.maxRequests()).isEqualTo(10);
        assertThat(result.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void checkAllowed_shouldLimitAiCommandToThreeRequestsPerMinute() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(clock);
        Long chatId = 123L;

        assertThat(limiter.checkAllowed(chatId, "/ai BTC").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai ETH").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai SOL").allowed()).isTrue();

        UserCommandRateLimiter.RateLimitResult result = limiter.checkAllowed(chatId, "/ai BNB");

        assertThat(result.allowed()).isFalse();
        assertThat(result.ruleName()).isEqualTo("AI analysis");
        assertThat(result.maxRequests()).isEqualTo(3);
    }

    @Test
    void checkAllowed_shouldLimitAiChartCommandToTwoRequestsPerMinute() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(clock);
        Long chatId = 123L;

        assertThat(limiter.checkAllowed(chatId, "/ai_chart BTC").allowed()).isTrue();
        assertThat(limiter.checkAllowed(chatId, "/ai_chart ETH").allowed()).isTrue();

        UserCommandRateLimiter.RateLimitResult result = limiter.checkAllowed(chatId, "/ai_chart SOL");

        assertThat(result.allowed()).isFalse();
        assertThat(result.ruleName()).isEqualTo("AI chart");
        assertThat(result.maxRequests()).isEqualTo(2);
    }

    @Test
    void checkAllowed_shouldAllowCommandsAgainAfterWindowExpires() {
        MutableClock clock = new MutableClock();
        UserCommandRateLimiter limiter = new UserCommandRateLimiter(clock);
        Long chatId = 123L;

        for (int index = 0; index < 10; index++) {
            assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isTrue();
        }
        assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(61));

        assertThat(limiter.checkAllowed(chatId, "/crypto BTC").allowed()).isTrue();
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
}
