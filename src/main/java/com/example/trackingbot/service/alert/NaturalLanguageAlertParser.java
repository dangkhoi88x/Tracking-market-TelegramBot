package com.example.trackingbot.service.alert;

import com.example.trackingbot.model.ParsedAlert;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NaturalLanguageAlertParser {

    private static final Map<String, String> SYMBOL_ALIASES = Map.ofEntries(
            Map.entry("btc", "BTC"),
            Map.entry("bitcoin", "BTC"),
            Map.entry("eth", "ETH"),
            Map.entry("ethereum", "ETH"),
            Map.entry("sol", "SOL"),
            Map.entry("solana", "SOL"),
            Map.entry("bnb", "BNB"),
            Map.entry("xrp", "XRP"),
            Map.entry("doge", "DOGE"),
            Map.entry("dogecoin", "DOGE")
    );
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
            "\\b(btc|bitcoin|eth|ethereum|sol|solana|bnb|xrp|doge|dogecoin)\\b"
    );
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\b(\\d+(?:[,.]\\d+)?\\s*[kKmM]?)\\b");

    public Optional<ParsedAlert> parse(String text) {
        if (text == null || text.isBlank() || text.trim().startsWith("/")) {
            return Optional.empty();
        }

        String normalized = normalize(text);
        Matcher symbolMatcher = SYMBOL_PATTERN.matcher(normalized);
        if (!symbolMatcher.find()) {
            return Optional.empty();
        }

        String symbol = SYMBOL_ALIASES.get(symbolMatcher.group(1));
        if (symbol == null) {
            return Optional.empty();
        }
        Optional<String> operator = findOperator(normalized);
        if (operator.isEmpty()) {
            return Optional.empty();
        }

        Optional<BigDecimal> price = findPriceAfterSymbol(normalized, symbolMatcher.end());
        return price.map(targetPrice -> new ParsedAlert(symbol, operator.get(), targetPrice));
    }

    public String toAlertArguments(ParsedAlert alert) {
        return "%s %s %s".formatted(
                alert.symbol(),
                alert.operator(),
                alert.targetPrice().stripTrailingZeros().toPlainString()
        );
    }

    private Optional<String> findOperator(String normalized) {
        if (normalized.contains(">")) {
            return Optional.of(">");
        }

        if (normalized.contains("<")) {
            return Optional.of("<");
        }

        if (containsAny(normalized, "vuot", "tren", "lon hon", "cao hon", "above", "over", "break")) {
            return Optional.of(">");
        }

        if (containsAny(normalized, "duoi", "nho hon", "thap hon", "below", "under")) {
            return Optional.of("<");
        }

        return Optional.empty();
    }

    private Optional<BigDecimal> findPriceAfterSymbol(String normalized, int symbolEndIndex) {
        String afterSymbol = normalized.substring(symbolEndIndex);
        Matcher priceMatcher = PRICE_PATTERN.matcher(afterSymbol);
        if (!priceMatcher.find()) {
            return Optional.empty();
        }

        return parsePrice(priceMatcher.group(1));
    }

    private Optional<BigDecimal> parsePrice(String rawPrice) {
        try {
            String normalized = rawPrice.trim().replace(" ", "").toLowerCase(Locale.ROOT);
            BigDecimal multiplier = BigDecimal.ONE;

            if (normalized.endsWith("k")) {
                multiplier = BigDecimal.valueOf(1_000);
                normalized = normalized.substring(0, normalized.length() - 1);
            } else if (normalized.endsWith("m")) {
                multiplier = BigDecimal.valueOf(1_000_000);
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            BigDecimal value = new BigDecimal(normalizeNumericPrice(normalized));
            if (value.signum() <= 0) {
                return Optional.empty();
            }

            return Optional.of(value.multiply(multiplier));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String normalizeNumericPrice(String normalized) {
        if (normalized.matches("\\d{1,3}([,.]\\d{3})+")) {
            return normalized.replaceAll("[,.]", "");
        }

        return normalized.replace(",", "");
    }

    private boolean containsAny(String normalized, String... keywords) {
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {
        String lowerCase = text.toLowerCase(Locale.ROOT);
        String withoutAccents = Normalizer.normalize(lowerCase, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("\\s+", " ").trim();
    }
}
