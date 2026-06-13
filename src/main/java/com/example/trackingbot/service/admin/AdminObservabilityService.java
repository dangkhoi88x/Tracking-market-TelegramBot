package com.example.trackingbot.service.admin;

import com.example.trackingbot.config.TelegramBotProperties;
import com.example.trackingbot.entity.CommandLogEntity;
import com.example.trackingbot.repository.CommandLogRepository;
import com.example.trackingbot.repository.DailySettingRepository;
import com.example.trackingbot.repository.PortfolioPositionRepository;
import com.example.trackingbot.repository.PriceAlertRepository;
import com.example.trackingbot.repository.TelegramUserRepository;
import com.example.trackingbot.repository.WatchlistItemRepository;
import com.example.trackingbot.model.SubscriptionPlan;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminObservabilityService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd-MM HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final List<String> CIRCUIT_BREAKERS = List.of(
            "coingecko",
            "binanceFutures",
            "binanceP2P",
            "openai"
    );

    private final TelegramBotProperties telegramBotProperties;
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TelegramUserRepository telegramUserRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final DailySettingRepository dailySettingRepository;
    private final PortfolioPositionRepository portfolioPositionRepository;
    private final ThreadPoolMetricsService threadPoolMetricsService;
    private final CommandLogRepository commandLogRepository;

    public String getHealthMessage(Long chatId) {
        String accessDeniedMessage = getAccessDeniedMessage(chatId);
        if (accessDeniedMessage != null) {
            return accessDeniedMessage;
        }

        return """
                Admin Health

                DB: %s
                Redis: %s
                CoinGecko CB: %s
                Binance Futures CB: %s
                Binance P2P CB: %s
                OpenAI CB: %s
                """.formatted(
                checkDatabase(),
                checkRedis(),
                circuitBreakerState("coingecko"),
                circuitBreakerState("binanceFutures"),
                circuitBreakerState("binanceP2P"),
                circuitBreakerState("openai")
        );
    }

    public String getMetricsMessage(Long chatId) {
        String accessDeniedMessage = getAccessDeniedMessage(chatId);
        if (accessDeniedMessage != null) {
            return accessDeniedMessage;
        }

        long activeAlerts = priceAlertRepository.findByActiveTrueOrderByCreatedAtAsc().size();
        long dailySubscribers = dailySettingRepository.findByEnabledTrueOrderByUserChatIdAsc().size();

        return """
                Admin Metrics

                Users: %d
                Plans: FREE %d | PRO %d | ADMIN %d
                Watchlist items: %d
                Active alerts: %d
                Daily subscribers: %d
                Portfolio positions: %d

                Thread Pools:
                %s

                Circuit Breakers:
                %s
                """.formatted(
                telegramUserRepository.count(),
                telegramUserRepository.countByPlan(SubscriptionPlan.FREE),
                telegramUserRepository.countByPlan(SubscriptionPlan.PRO),
                telegramUserRepository.countByPlan(SubscriptionPlan.ADMIN),
                watchlistItemRepository.count(),
                activeAlerts,
                dailySubscribers,
                portfolioPositionRepository.count(),
                threadPoolMetricsService.buildMetricsMessage(),
                buildCircuitBreakerMetrics()
        );
    }

    public String getTopCommandsMessage(Long chatId) {
        String accessDeniedMessage = getAccessDeniedMessage(chatId);
        if (accessDeniedMessage != null) {
            return accessDeniedMessage;
        }

        Instant since = Instant.now().minus(Duration.ofDays(7));
        List<CommandLogRepository.TopCommandProjection> commands = commandLogRepository.findTopCommandsSince(
                since,
                PageRequest.of(0, 10)
        );

        if (commands.isEmpty()) {
            return "Chua co command log trong 7 ngay gan day.";
        }

        StringBuilder message = new StringBuilder("Top commands 7 ngay gan day:\n\n");
        for (int index = 0; index < commands.size(); index++) {
            CommandLogRepository.TopCommandProjection command = commands.get(index);
            message.append("%d. %s: %d calls | ok %d | err %d | avg %.0fms%n".formatted(
                    index + 1,
                    command.getCommand(),
                    command.getTotalCount(),
                    command.getSuccessCount(),
                    command.getErrorCount(),
                    command.getAverageDurationMs() == null ? 0 : command.getAverageDurationMs()
            ));
        }

        return message.toString().trim();
    }

    public String getErrorsMessage(Long chatId) {
        String accessDeniedMessage = getAccessDeniedMessage(chatId);
        if (accessDeniedMessage != null) {
            return accessDeniedMessage;
        }

        List<CommandLogEntity> errors = commandLogRepository.findRecentErrors(PageRequest.of(0, 10));
        if (errors.isEmpty()) {
            return "Chua co command error nao duoc ghi nhan.";
        }

        StringBuilder message = new StringBuilder("Recent command errors:\n\n");
        for (CommandLogEntity error : errors) {
            message.append("%s | chat %d | %s | %dms%n%s%n%n".formatted(
                    TIME_FORMATTER.format(error.getCreatedAt()),
                    error.getChatId(),
                    error.getCommand(),
                    error.getDurationMs(),
                    safeErrorMessage(error.getErrorMessage())
            ));
        }

        return message.toString().trim();
    }

    public String getUsersMessage(Long chatId) {
        String accessDeniedMessage = getAccessDeniedMessage(chatId);
        if (accessDeniedMessage != null) {
            return accessDeniedMessage;
        }

        Instant since = Instant.now().minus(Duration.ofDays(7));
        List<CommandLogRepository.TopUserProjection> users = commandLogRepository.findTopUsersSince(
                since,
                PageRequest.of(0, 10)
        );

        StringBuilder message = new StringBuilder("""
                Admin Users

                Registered users: %d
                Users with command logs: %d
                Active users 7d: %d

                Top active users 7d:
                """.formatted(
                telegramUserRepository.count(),
                commandLogRepository.countDistinctUsers(),
                commandLogRepository.countDistinctUsersSince(since)
        ));

        if (users.isEmpty()) {
            message.append("Chua co user nao dung command trong 7 ngay gan day.");
            return message.toString();
        }

        for (int index = 0; index < users.size(); index++) {
            CommandLogRepository.TopUserProjection user = users.get(index);
            message.append("%d. chat %d: %d commands | last %s%n".formatted(
                    index + 1,
                    user.getChatId(),
                    user.getTotalCount(),
                    TIME_FORMATTER.format(user.getLastCommandAt())
            ));
        }

        return message.toString().trim();
    }

    private String getAccessDeniedMessage(Long chatId) {
        String adminChatId = telegramBotProperties.adminChatId();
        if (adminChatId == null || adminChatId.isBlank()) {
            return """
                    Chua cau hinh TELEGRAM_ADMIN_CHAT_ID.

                    De bat lenh admin, lay chat_id cua ban trong bang telegram_users roi them env:
                    TELEGRAM_ADMIN_CHAT_ID=chat_id_cua_ban
                    """;
        }

        if (!adminChatId.equals(String.valueOf(chatId))) {
            return "Lenh admin chi danh cho owner cua bot.";
        }

        return null;
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception exception) {
            return "DOWN (" + exception.getClass().getSimpleName() + ")";
        }
    }

    private String checkRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String response = connection.ping();
            return "PONG".equalsIgnoreCase(response) ? "UP" : "DOWN";
        } catch (Exception exception) {
            return "DOWN (" + exception.getClass().getSimpleName() + ")";
        }
    }

    private String circuitBreakerState(String name) {
        return circuitBreakerRegistry.find(name)
                .map(circuitBreaker -> circuitBreaker.getState().name())
                .orElse("NOT_FOUND");
    }

    private String buildCircuitBreakerMetrics() {
        StringBuilder builder = new StringBuilder();
        for (String name : CIRCUIT_BREAKERS) {
            circuitBreakerRegistry.find(name).ifPresentOrElse(
                    circuitBreaker -> builder.append("%s: %s | failure %.1f%% | slow %.1f%% | calls %d%n".formatted(
                            name,
                            circuitBreaker.getState().name(),
                            circuitBreaker.getMetrics().getFailureRate(),
                            circuitBreaker.getMetrics().getSlowCallRate(),
                            circuitBreaker.getMetrics().getNumberOfBufferedCalls()
                    )),
                    () -> builder.append("%s: NOT_FOUND%n".formatted(name))
            );
        }

        return builder.toString().stripTrailing();
    }

    private String safeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "No error message";
        }

        return errorMessage.length() <= 250 ? errorMessage : errorMessage.substring(0, 250) + "...";
    }
}
