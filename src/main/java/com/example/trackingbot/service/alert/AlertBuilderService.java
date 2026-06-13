package com.example.trackingbot.service.alert;

import com.example.trackingbot.dto.request.InlineKeyboardButton;
import com.example.trackingbot.dto.request.InlineKeyboardMarkup;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertBuilderService {

    private static final List<String> POPULAR_SYMBOLS = List.of("BTC", "ETH", "SOL", "BNB", "XRP", "DOGE");

    private final CryptoPriceService cryptoPriceService;
    private final AlertService alertService;

    public String getStartMessage() {
        return """
                Smart Alert Builder

                Chon coin ban muon tao alert:
                """;
    }

    public InlineKeyboardMarkup buildSymbolKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        symbolButton("BTC"),
                        symbolButton("ETH"),
                        symbolButton("SOL")
                ),
                List.of(
                        symbolButton("BNB"),
                        symbolButton("XRP"),
                        symbolButton("DOGE")
                )
        ));
    }

    public String getOperatorMessage(String symbol) {
        return """
                Tao alert cho %s

                Ban muon bot bao khi gia:
                """.formatted(symbol);
    }

    public InlineKeyboardMarkup buildOperatorKeyboard(String symbol) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Above", "ALERT_BUILDER:OPERATOR:" + symbol + ":GT"),
                        new InlineKeyboardButton("Below", "ALERT_BUILDER:OPERATOR:" + symbol + ":LT")
                )
        ));
    }

    public String getPercentMessage(String symbol, String operatorCode) {
        BigDecimal currentPrice = cryptoPriceService.getCurrentPrice(symbol).priceUsd();
        return """
                %s hien tai: %s USD

                Chon muc %s so voi gia hien tai:
                """.formatted(
                symbol,
                formatPrice(currentPrice),
                "GT".equals(operatorCode) ? "cao hon" : "thap hon"
        );
    }

    public InlineKeyboardMarkup buildPercentKeyboard(String symbol, String operatorCode) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        percentButton(symbol, operatorCode, 1),
                        percentButton(symbol, operatorCode, 3),
                        percentButton(symbol, operatorCode, 5)
                ),
                List.of(
                        new InlineKeyboardButton("Custom Price", "ALERT_BUILDER:CUSTOM:" + symbol + ":" + operatorCode)
                )
        ));
    }

    public String createPercentAlert(Long chatId, String symbol, String operatorCode, int percent) {
        if (!POPULAR_SYMBOLS.contains(symbol) || percent <= 0 || percent > 100) {
            throw new IllegalArgumentException("Invalid alert builder input");
        }

        BigDecimal currentPrice = cryptoPriceService.getCurrentPrice(symbol).priceUsd();
        String operator = toOperator(operatorCode);
        BigDecimal targetPrice = calculateTargetPrice(currentPrice, operatorCode, percent);

        return alertService.createAlert(
                chatId,
                "%s %s %s".formatted(symbol, operator, targetPrice.stripTrailingZeros().toPlainString())
        );
    }

    public String getCustomPriceMessage(String symbol, String operatorCode) {
        return """
                Nhap gia custom bang lenh:

                /alert %s %s 70000

                Hoac go tu nhien:
                nhac toi khi %s %s 70000
                """.formatted(
                symbol,
                toOperator(operatorCode),
                symbol.toLowerCase(),
                "GT".equals(operatorCode) ? "vuot" : "duoi"
        );
    }

    public boolean isSupportedSymbol(String symbol) {
        return POPULAR_SYMBOLS.contains(symbol);
    }

    public boolean isSupportedOperatorCode(String operatorCode) {
        return "GT".equals(operatorCode) || "LT".equals(operatorCode);
    }

    private InlineKeyboardButton symbolButton(String symbol) {
        return new InlineKeyboardButton(symbol, "ALERT_BUILDER:SYMBOL:" + symbol);
    }

    private InlineKeyboardButton percentButton(String symbol, String operatorCode, int percent) {
        String label = "GT".equals(operatorCode) ? "+" + percent + "%" : "-" + percent + "%";
        return new InlineKeyboardButton(label, "ALERT_BUILDER:PERCENT:" + symbol + ":" + operatorCode + ":" + percent);
    }

    private BigDecimal calculateTargetPrice(BigDecimal currentPrice, String operatorCode, int percent) {
        BigDecimal percentMultiplier = BigDecimal.valueOf(percent)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        BigDecimal multiplier = "GT".equals(operatorCode)
                ? BigDecimal.ONE.add(percentMultiplier)
                : BigDecimal.ONE.subtract(percentMultiplier);

        return currentPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private String toOperator(String operatorCode) {
        return switch (operatorCode) {
            case "GT" -> ">";
            case "LT" -> "<";
            default -> throw new IllegalArgumentException("Unsupported operator code: " + operatorCode);
        };
    }

    private String formatPrice(BigDecimal value) {
        return value == null ? "N/A" : "%,.2f".formatted(value);
    }
}
