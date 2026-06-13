package com.example.trackingbot.service.telegram;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NaturalLanguageCommandParser {

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
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d+(?:[,.]\\d+)?\\s*[kKmM]?)\\b");
    private static final Pattern EXPLICIT_PERIOD_PATTERN = Pattern.compile("\\b(30m|1h|24h|1d|7d|30d|1m)\\b");
    private static final Pattern NATURAL_PERIOD_PATTERN = Pattern.compile(
            "\\b(1|7|24|30)\\s*(phut|minute|minutes|gio|hour|hours|ngay|day|days|thang|month|months)\\b"
    );

    public Optional<NaturalLanguageCommand> parse(String text) {
        if (text == null || text.isBlank() || text.trim().startsWith("/")) {
            return Optional.empty();
        }

        String normalized = normalize(text);
        Optional<String> symbol = findSymbol(normalized);
        if (symbol.isEmpty()) {
            return Optional.empty();
        }

        if (isBuyIntent(normalized)) {
            Optional<NaturalLanguageCommand> buyCommand = parsePortfolioIntent(
                    NaturalLanguageCommandType.BUY,
                    symbol.get(),
                    normalized
            );
            if (buyCommand.isPresent()) {
                return buyCommand;
            }
        }

        if (isSellIntent(normalized)) {
            Optional<NaturalLanguageCommand> sellCommand = parsePortfolioIntent(
                    NaturalLanguageCommandType.SELL,
                    symbol.get(),
                    normalized
            );
            if (sellCommand.isPresent()) {
                return sellCommand;
            }
        }

        if (isChartIntent(normalized)) {
            return Optional.of(new NaturalLanguageCommand(
                    NaturalLanguageCommandType.CHART,
                    symbol.get(),
                    findPeriod(normalized)
            ));
        }

        if (isPriceIntent(normalized)) {
            return Optional.of(new NaturalLanguageCommand(
                    NaturalLanguageCommandType.PRICE,
                    symbol.get(),
                    ""
            ));
        }

        return Optional.empty();
    }

    private Optional<NaturalLanguageCommand> parsePortfolioIntent(
            NaturalLanguageCommandType type,
            String symbol,
            String normalized
    ) {
        List<BigDecimal> numbers = findNumbers(normalized);
        if (numbers.isEmpty()) {
            return Optional.empty();
        }

        String arguments;
        if (numbers.size() >= 2) {
            BigDecimal amount = numbers.get(0);
            BigDecimal entryPrice = numbers.get(numbers.size() - 1);
            arguments = "%s %s %s".formatted(symbol, formatNumber(amount), formatNumber(entryPrice));
        } else {
            arguments = "%s %s".formatted(symbol, formatNumber(numbers.get(0)));
        }

        return Optional.of(new NaturalLanguageCommand(type, symbol, arguments));
    }

    private Optional<String> findSymbol(String normalized) {
        Matcher matcher = SYMBOL_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.ofNullable(SYMBOL_ALIASES.get(matcher.group(1)));
    }

    private String findPeriod(String normalized) {
        Matcher explicitMatcher = EXPLICIT_PERIOD_PATTERN.matcher(normalized);
        if (explicitMatcher.find()) {
            return explicitMatcher.group(1);
        }

        Matcher naturalMatcher = NATURAL_PERIOD_PATTERN.matcher(normalized);
        if (!naturalMatcher.find()) {
            return "7d";
        }

        String value = naturalMatcher.group(1);
        String unit = naturalMatcher.group(2);
        if ("phut".equals(unit) || unit.startsWith("minute")) {
            return "30".equals(value) ? "30m" : "30m";
        }

        if ("gio".equals(unit) || unit.startsWith("hour")) {
            return "24".equals(value) ? "24h" : "1h";
        }

        if ("ngay".equals(unit) || unit.startsWith("day")) {
            return switch (value) {
                case "1" -> "1d";
                case "7" -> "7d";
                case "30" -> "30d";
                default -> "7d";
            };
        }

        if ("thang".equals(unit) || unit.startsWith("month")) {
            return "1m";
        }

        return "7d";
    }

    private List<BigDecimal> findNumbers(String normalized) {
        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        List<BigDecimal> numbers = new ArrayList<>();
        while (matcher.find()) {
            parseNumber(matcher.group(1)).ifPresent(numbers::add);
        }

        return numbers;
    }

    private Optional<BigDecimal> parseNumber(String rawNumber) {
        try {
            String normalized = rawNumber.trim().replace(" ", "").toLowerCase(Locale.ROOT);
            BigDecimal multiplier = BigDecimal.ONE;

            if (normalized.endsWith("k")) {
                multiplier = BigDecimal.valueOf(1_000);
                normalized = normalized.substring(0, normalized.length() - 1);
            } else if (normalized.endsWith("m")) {
                multiplier = BigDecimal.valueOf(1_000_000);
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            BigDecimal value = new BigDecimal(normalizeNumericValue(normalized));
            if (value.signum() <= 0) {
                return Optional.empty();
            }

            return Optional.of(value.multiply(multiplier));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String normalizeNumericValue(String normalized) {
        if (normalized.matches("\\d{1,3}([,.]\\d{3})+")) {
            return normalized.replaceAll("[,.]", "");
        }

        return normalized.replace(",", "");
    }

    private boolean isBuyIntent(String normalized) {
        return containsAny(normalized, "mua", "buy", "long");
    }

    private boolean isSellIntent(String normalized) {
        return containsAny(normalized, "ban", "sell", "short");
    }

    private boolean isChartIntent(String normalized) {
        return containsAny(normalized, "chart", "bieu do", "ve chart", "ve bieu do", "xem chart");
    }

    private boolean isPriceIntent(String normalized) {
        return containsAny(normalized, "gia", "price", "bao nhieu", "check");
    }

    private boolean containsAny(String normalized, String... keywords) {
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String formatNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String normalize(String text) {
        String lowerCase = text.toLowerCase(Locale.ROOT);
        String withoutAccents = Normalizer.normalize(lowerCase, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("\\s+", " ").trim();
    }

    public record NaturalLanguageCommand(
            NaturalLanguageCommandType type,
            String symbol,
            String arguments
    ) {
    }

    public enum NaturalLanguageCommandType {
        PRICE,
        CHART,
        BUY,
        SELL
    }
}
