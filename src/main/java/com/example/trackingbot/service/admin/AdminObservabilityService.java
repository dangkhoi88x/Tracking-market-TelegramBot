package com.example.trackingbot.service.admin;

import com.example.trackingbot.config.TelegramBotProperties;
import com.example.trackingbot.repository.DailySettingRepository;
import com.example.trackingbot.repository.PortfolioPositionRepository;
import com.example.trackingbot.repository.PriceAlertRepository;
import com.example.trackingbot.repository.TelegramUserRepository;
import com.example.trackingbot.repository.WatchlistItemRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminObservabilityService {

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
                watchlistItemRepository.count(),
                activeAlerts,
                dailySubscribers,
                portfolioPositionRepository.count(),
                threadPoolMetricsService.buildMetricsMessage(),
                buildCircuitBreakerMetrics()
        );
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
}
