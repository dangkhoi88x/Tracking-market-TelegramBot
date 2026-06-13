package com.example.trackingbot.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserCommandRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(UserCommandRateLimiter.class);

    private static final RateLimitRule GLOBAL_RULE = new RateLimitRule(
            "*",
            20,
            Duration.ofMinutes(1),
            "Tat ca lenh",
            "phut"
    );
    private static final Map<String, RateLimitRule> COMMAND_RULES = Map.of(
            "/ai", new RateLimitRule("/ai", 3, Duration.ofDays(1), "AI analysis", "ngay"),
            "/ai_chart", new RateLimitRule("/ai_chart", 2, Duration.ofDays(1), "AI chart", "ngay")
    );

    private final RateLimitStore rateLimitStore;
    private final Clock clock;

    @Autowired
    public UserCommandRateLimiter(RateLimitStore rateLimitStore) {
        this(rateLimitStore, Clock.systemUTC());
    }

    UserCommandRateLimiter(RateLimitStore rateLimitStore, Clock clock) {
        this.rateLimitStore = rateLimitStore;
        this.clock = clock;
    }

    public RateLimitResult checkAllowed(Long chatId, String commandText) {
        String command = extractCommand(commandText);
        if (chatId == null || command.isBlank()) {
            return RateLimitResult.allow();
        }

        List<RateLimitRule> rules = buildRules(command);
        List<RateLimitStore.RateLimitStoreRequest> requests = buildStoreRequests(chatId, rules);

        try {
            RateLimitStore.RateLimitStoreResult storeResult = rateLimitStore.checkAndIncrement(requests);
            if (storeResult.allowed()) {
                return RateLimitResult.allow();
            }

            RateLimitRule rejectedRule = rules.get(storeResult.rejectedIndex());
            return RateLimitResult.reject(
                    rejectedRule.label(),
                    rejectedRule.maxRequests(),
                    rejectedRule.windowLabel(),
                    storeResult.retryAfterSeconds()
            );
        } catch (RuntimeException exception) {
            return handleRateLimitStoreFailure(command, exception);
        }
    }

    private List<RateLimitRule> buildRules(String command) {
        List<RateLimitRule> rules = new ArrayList<>();
        rules.add(GLOBAL_RULE);

        RateLimitRule commandRule = COMMAND_RULES.get(command);
        if (commandRule != null) {
            rules.add(commandRule);
        }

        return rules;
    }

    private List<RateLimitStore.RateLimitStoreRequest> buildStoreRequests(Long chatId, List<RateLimitRule> rules) {
        Instant now = Instant.now(clock);
        return rules.stream()
                .map(rule -> new RateLimitStore.RateLimitStoreRequest(
                        buildRedisKey(chatId, rule.key()),
                        rule.maxRequests(),
                        rule.window(),
                        now
                ))
                .toList();
    }

    private RateLimitResult handleRateLimitStoreFailure(String command, RuntimeException exception) {
        RateLimitRule commandRule = COMMAND_RULES.get(command);
        if (commandRule != null) {
            log.error("Redis rate limiter failed for protected command {}", command, exception);
            return RateLimitResult.reject(
                    commandRule.label(),
                    commandRule.maxRequests(),
                    commandRule.windowLabel(),
                    60
            );
        }

        log.warn("Redis rate limiter failed for normal command {}, allowing command", command, exception);
        return RateLimitResult.allow();
    }

    private String buildRedisKey(Long chatId, String ruleKey) {
        return "rate_limit:%d:%s".formatted(chatId, ruleKey);
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
            String windowLabel,
            long retryAfterSeconds
    ) {

        static RateLimitResult allow() {
            return new RateLimitResult(true, "", 0, "", 0);
        }

        static RateLimitResult reject(String ruleName, int maxRequests, String windowLabel, long retryAfterSeconds) {
            return new RateLimitResult(false, ruleName, maxRequests, windowLabel, retryAfterSeconds);
        }
    }

    private record RateLimitRule(
            String key,
            int maxRequests,
            Duration window,
            String label,
            String windowLabel
    ) {
    }
}
