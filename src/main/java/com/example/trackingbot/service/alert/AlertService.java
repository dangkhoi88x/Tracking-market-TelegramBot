package com.example.trackingbot.service.alert;

import com.example.trackingbot.model.CryptoAlert;
import com.example.trackingbot.entity.PriceAlertEntity;
import com.example.trackingbot.repository.PriceAlertRepository;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.example.trackingbot.service.telegram.TelegramUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final CryptoPriceService cryptoPriceService;
    private final TelegramUserService telegramUserService;
    private final PriceAlertRepository priceAlertRepository;

    @Transactional
    public String createAlert(Long chatId, String rawArguments) {
        AlertCommand command = parseAlertCommand(rawArguments);
        String symbol = cryptoPriceService.normalizeSymbol(command.symbol());

        if (cryptoPriceService.findCoinId(symbol).isEmpty()) {
            return unsupportedSymbolMessage();
        }

        String alertId = UUID.randomUUID().toString();
        priceAlertRepository.save(new PriceAlertEntity(
                alertId,
                telegramUserService.getOrCreateUser(chatId),
                symbol,
                command.operator(),
                command.targetPrice()
        ));

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

    @Transactional(readOnly = true)
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

    @Transactional
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

        priceAlertRepository.findById(alertId).ifPresent(PriceAlertEntity::markDeleted);

        return "Da xoa alert " + alertId + ".";
    }

    @Transactional(readOnly = true)
    public List<CryptoAlert> getActiveAlerts() {
        return priceAlertRepository.findByActiveTrueOrderByCreatedAtAsc()
                .stream()
                .map(this::toCryptoAlert)
                .toList();
    }

    private List<CryptoAlert> getUserActiveAlerts(Long chatId) {
        return priceAlertRepository.findByUserChatIdAndActiveTrueOrderByCreatedAtDesc(chatId)
                .stream()
                .map(this::toCryptoAlert)
                .toList();
    }

    @Transactional
    public void markTriggered(String alertId) {
        priceAlertRepository.findById(alertId).ifPresent(PriceAlertEntity::markTriggered);
    }

    private Optional<CryptoAlert> findAlertById(String alertId) {
        return priceAlertRepository.findById(alertId)
                .map(this::toCryptoAlert);
    }

    private CryptoAlert toCryptoAlert(PriceAlertEntity alert) {
        return new CryptoAlert(
                alert.getId(),
                alert.getUser().getChatId(),
                alert.getSymbol(),
                alert.getOperator(),
                alert.getTargetPrice(),
                alert.isActive()
        );
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
