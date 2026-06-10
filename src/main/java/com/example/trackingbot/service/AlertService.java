package com.example.trackingbot.service;

import com.example.trackingbot.dto.CryptoAlert;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AlertService {

    private static final String ALERT_KEY_PREFIX = "alert:";
    private static final String ACTIVE_ALERTS_KEY = "alerts:active";

    private final StringRedisTemplate redisTemplate;
    private final CryptoPriceService cryptoPriceService;

    public AlertService(StringRedisTemplate redisTemplate, CryptoPriceService cryptoPriceService) {
        this.redisTemplate = redisTemplate;
        this.cryptoPriceService = cryptoPriceService;
    }

    public String createAlert(Long chatId, String rawArguments) {
        AlertCommand command = parseAlertCommand(rawArguments);
        String symbol = cryptoPriceService.normalizeSymbol(command.symbol());

        if (cryptoPriceService.findCoinId(symbol).isEmpty()) {
            return unsupportedSymbolMessage();
        }

        String alertId = UUID.randomUUID().toString();
        String alertKey = alertKey(alertId);

        redisTemplate.opsForHash().putAll(alertKey, Map.of(
                "id", alertId,
                "chatId", chatId.toString(),
                "symbol", symbol,
                "operator", command.operator(),
                "targetPrice", command.targetPrice().toPlainString(),
                "active", "true",
                "createdAt", Instant.now().toString()
        ));
        redisTemplate.opsForSet().add(ACTIVE_ALERTS_KEY, alertId);

        return """
                Da tao alert.

                %s %s %s USD
                Alert id: %s
                """.formatted(
                symbol,
                command.operator(),
                command.targetPrice().toPlainString(),
                alertId
        );
    }

    public String createNotification(Long chatId, String rawArguments) {
        NotificationCommand command = parseNotificationCommand(rawArguments);
        String symbol = cryptoPriceService.normalizeSymbol(command.symbol());

        if (cryptoPriceService.findCoinId(symbol).isEmpty()) {
            return unsupportedSymbolMessage();
        }

        BigDecimal currentPrice = cryptoPriceService.getCurrentPrice(symbol).priceUsd();
        String operator = currentPrice.compareTo(command.targetPrice()) <= 0 ? ">=" : "<=";

        return createAlert(chatId, "%s %s %s".formatted(
                symbol,
                operator,
                command.targetPrice().toPlainString()
        ));
    }

    public String getHelpMessage() {
        return """
                Cach dung:
                /alert BTC > 70000
                /alert ETH < 3000
                /myalerts
                /delete_alert ALERT_ID

                Toan tu ho tro:
                >, <, >=, <=

                Ma crypto dang ho tro:
                %s
                """.formatted(cryptoPriceService.getSupportedSymbolsText());
    }

    public String getUserAlertsMessage(Long chatId) {
        List<CryptoAlert> userAlerts = getUserActiveAlerts(chatId);
        if (userAlerts.isEmpty()) {
            return """
                    Ban chua co alert nao dang active.

                    Tao alert moi:
                    /alert BTC > 70000
                    """;
        }

        StringBuilder message = new StringBuilder("Alert dang active cua ban:\n\n");
        for (CryptoAlert alert : userAlerts) {
            message.append("%s %s %s USD\n".formatted(
                    alert.symbol(),
                    alert.operator(),
                    alert.targetPrice().toPlainString()
            ));
            message.append("ID: ").append(alert.id()).append("\n\n");
        }

        message.append("Xoa alert:\n/delete_alert ALERT_ID");
        return message.toString();
    }

    public String deleteAlert(Long chatId, String rawAlertId) {
        String alertId = rawAlertId == null ? "" : rawAlertId.trim();
        if (alertId.isBlank()) {
            return """
                    Ban hay nhap alert id can xoa.

                    Vi du:
                    /delete_alert ALERT_ID
                    """;
        }

        Optional<CryptoAlert> alert = findAlertById(alertId);
        if (alert.isEmpty() || !alert.get().active()) {
            return "Khong tim thay alert dang active voi id nay.";
        }

        if (!alert.get().chatId().equals(chatId)) {
            return "Ban khong co quyen xoa alert nay.";
        }

        redisTemplate.opsForHash().put(alertKey(alertId), "active", "false");
        redisTemplate.opsForHash().put(alertKey(alertId), "deletedAt", Instant.now().toString());
        redisTemplate.opsForSet().remove(ACTIVE_ALERTS_KEY, alertId);

        return "Da xoa alert " + alertId + ".";
    }

    public List<CryptoAlert> getActiveAlerts() {
        Set<String> alertIds = redisTemplate.opsForSet().members(ACTIVE_ALERTS_KEY);
        return Optional.ofNullable(alertIds)
                .orElse(Collections.emptySet())
                .stream()
                .map(this::findAlertById)
                .flatMap(Optional::stream)
                .filter(CryptoAlert::active)
                .toList();
    }

    private List<CryptoAlert> getUserActiveAlerts(Long chatId) {
        Set<String> alertKeys = redisTemplate.keys(ALERT_KEY_PREFIX + "*");
        return Optional.ofNullable(alertKeys)
                .orElse(Collections.emptySet())
                .stream()
                .map(this::alertIdFromKey)
                .map(this::findAlertById)
                .flatMap(Optional::stream)
                .filter(CryptoAlert::active)
                .filter(alert -> alert.chatId().equals(chatId))
                .toList();
    }

    private String alertIdFromKey(String alertKey) {
        return alertKey.substring(ALERT_KEY_PREFIX.length());
    }

    public void markTriggered(String alertId) {
        redisTemplate.opsForHash().put(alertKey(alertId), "active", "false");
        redisTemplate.opsForHash().put(alertKey(alertId), "triggeredAt", Instant.now().toString());
        redisTemplate.opsForSet().remove(ACTIVE_ALERTS_KEY, alertId);
    }

    private Optional<CryptoAlert> findAlertById(String alertId) {
        Map<Object, Object> rawAlert = redisTemplate.opsForHash().entries(alertKey(alertId));
        if (rawAlert.isEmpty()) {
            redisTemplate.opsForSet().remove(ACTIVE_ALERTS_KEY, alertId);
            return Optional.empty();
        }

        try {
            return Optional.of(new CryptoAlert(
                    value(rawAlert, "id"),
                    Long.parseLong(value(rawAlert, "chatId")),
                    value(rawAlert, "symbol"),
                    value(rawAlert, "operator"),
                    new BigDecimal(value(rawAlert, "targetPrice")),
                    Boolean.parseBoolean(value(rawAlert, "active"))
            ));
        } catch (RuntimeException exception) {
            redisTemplate.opsForSet().remove(ACTIVE_ALERTS_KEY, alertId);
            return Optional.empty();
        }
    }

    private String value(Map<Object, Object> rawAlert, String field) {
        Object value = rawAlert.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Missing alert field: " + field);
        }

        return value.toString();
    }

    private AlertCommand parseAlertCommand(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            throw new IllegalArgumentException("Missing alert arguments");
        }

        String[] parts = rawArguments.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid alert format");
        }

        String symbol = parts[0];
        String operator = parts[1];
        BigDecimal targetPrice = parsePrice(parts[2]);

        if (!isSupportedOperator(operator)) {
            throw new IllegalArgumentException("Unsupported alert operator");
        }

        if (targetPrice.signum() <= 0) {
            throw new IllegalArgumentException("Alert target price must be positive");
        }

        return new AlertCommand(symbol, operator, targetPrice);
    }

    private NotificationCommand parseNotificationCommand(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            throw new IllegalArgumentException("Missing notification arguments");
        }

        String[] parts = rawArguments.trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid notification format");
        }

        BigDecimal targetPrice = parsePrice(parts[1]);
        if (targetPrice.signum() <= 0) {
            throw new IllegalArgumentException("Notification target price must be positive");
        }

        return new NotificationCommand(parts[0], targetPrice);
    }

    private BigDecimal parsePrice(String rawPrice) {
        try {
            return new BigDecimal(rawPrice.replace(",", ""));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid alert target price", exception);
        }
    }

    private boolean isSupportedOperator(String operator) {
        return ">".equals(operator)
                || "<".equals(operator)
                || ">=".equals(operator)
                || "<=".equals(operator);
    }

    private String unsupportedSymbolMessage() {
        return """
                Minh chua ho tro ma crypto nay.

                Ma crypto dang ho tro:
                %s
                """.formatted(cryptoPriceService.getSupportedSymbolsText());
    }

    private String alertKey(String alertId) {
        return ALERT_KEY_PREFIX + alertId;
    }

    private record AlertCommand(
            String symbol,
            String operator,
            BigDecimal targetPrice
    ) {
    }

    private record NotificationCommand(
            String symbol,
            BigDecimal targetPrice
    ) {
    }
}
