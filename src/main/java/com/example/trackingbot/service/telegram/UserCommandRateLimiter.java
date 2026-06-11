package com.example.trackingbot.service.telegram;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class UserCommandRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final RateLimitRule GLOBAL_RULE = new RateLimitRule("*", 10, "Tat ca lenh");
    private static final Map<String, RateLimitRule> COMMAND_RULES = Map.of(
            "/ai", new RateLimitRule("/ai", 3, "AI analysis"),
            "/ai_chart", new RateLimitRule("/ai_chart", 2, "AI chart")
    );

    private final Clock clock;
    private final ConcurrentMap<Long, UserRateLimitState> userStates = new ConcurrentHashMap<>();

    public UserCommandRateLimiter() {
        this(Clock.systemUTC());
    }

    UserCommandRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public RateLimitResult checkAllowed(Long chatId, String commandText) {
        String command = extractCommand(commandText);
        if (chatId == null || command.isBlank()) {
            return RateLimitResult.allow();
        }

        UserRateLimitState state = userStates.computeIfAbsent(chatId, ignored -> new UserRateLimitState());
        Instant now = Instant.now(clock);
        List<RateLimitRule> rules = buildRules(command);

        synchronized (state) {
            for (RateLimitRule rule : rules) {
                ArrayDeque<Instant> timestamps = state.timestampsByRule.computeIfAbsent(rule.key(), ignored -> new ArrayDeque<>());
                removeExpired(timestamps, now);
            }

            RateLimitResult rejectedResult = findRejectedResult(state, rules, now);
            if (!rejectedResult.allowed()) {
                return rejectedResult;
            }

            for (RateLimitRule rule : rules) {
                state.timestampsByRule.get(rule.key()).addLast(now);
            }
        }

        return RateLimitResult.allow();
    }

    private List<RateLimitRule> buildRules(String command) {
        List<RateLimitRule> rules = new ArrayList<>();
        RateLimitRule commandRule = COMMAND_RULES.get(command);
        if (commandRule != null) {
            rules.add(commandRule);
        }
        rules.add(GLOBAL_RULE);

        return rules;
    }

    private RateLimitResult findRejectedResult(UserRateLimitState state, List<RateLimitRule> rules, Instant now) {
        for (RateLimitRule rule : rules) {
            ArrayDeque<Instant> timestamps = state.timestampsByRule.get(rule.key());
            if (timestamps.size() >= rule.maxRequests()) {
                return RateLimitResult.reject(rule.label(), rule.maxRequests(), calculateRetryAfterSeconds(timestamps, now));
            }
        }

        return RateLimitResult.allow();
    }

    private void removeExpired(ArrayDeque<Instant> timestamps, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
            timestamps.removeFirst();
        }
    }

    private long calculateRetryAfterSeconds(ArrayDeque<Instant> timestamps, Instant now) {
        Instant retryAt = timestamps.peekFirst().plus(WINDOW);
        long retryAfterMillis = Duration.between(now, retryAt).toMillis();
        if (retryAfterMillis <= 0) {
            return 1;
        }

        return Math.max(1, (retryAfterMillis + 999) / 1000);
    }

    private String extractCommand(String commandText) {
        if (commandText == null || !commandText.trim().startsWith("/")) {
            return "";
        }

        return commandText.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }

    public record RateLimitResult(
            boolean allowed,
            String ruleName,
            int maxRequests,
            long retryAfterSeconds
    ) {

        static RateLimitResult allow() {
            return new RateLimitResult(true, "", 0, 0);
        }

        static RateLimitResult reject(String ruleName, int maxRequests, long retryAfterSeconds) {
            return new RateLimitResult(false, ruleName, maxRequests, retryAfterSeconds);
        }
    }

    private record RateLimitRule(
            String key,
            int maxRequests,
            String label
    ) {
    }

    private static class UserRateLimitState {

        private final Map<String, ArrayDeque<Instant>> timestampsByRule = new ConcurrentHashMap<>();
    }
}
