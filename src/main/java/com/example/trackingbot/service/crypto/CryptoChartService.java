package com.example.trackingbot.service.crypto;

import com.example.trackingbot.client.CoinGeckoClient;
import com.example.trackingbot.dto.response.CryptoChartImage;
import com.example.trackingbot.dto.response.CryptoChartPoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CryptoChartService {

    private static final String CRYPTO_CHART_CACHE = "cryptoCharts";
    private static final int MAX_CHART_POINTS = 28;

    private static final DateTimeFormatter SHORT_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final CoinGeckoClient coinGeckoClient;
    private final CryptoPriceService cryptoPriceService;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isHelpCommand(String rawArgument) {
        String argument = cryptoPriceService.normalizeSymbol(rawArgument);
        return argument.isBlank() || "HELP".equals(argument) || "?".equals(argument);
    }

    public String getHelpMessage() {
        return """
                Cach dung:
                /crypto_chart BTC 7d

                Khung thoi gian ho tro:
                30m, 1h, 24h, 1d, 7d, 30d, 1m

                Ma crypto dang ho tro:
                %s
                """.formatted(cryptoPriceService.getSupportedSymbolsText());
    }

    public CryptoChartImage getChartImage(String rawSymbol, String rawPeriod) {
        String symbol = cryptoPriceService.normalizeSymbol(rawSymbol);
        Optional<String> coinId = cryptoPriceService.findCoinId(symbol);
        ChartPeriod period = ChartPeriod.parse(rawPeriod);

        if (coinId.isEmpty()) {
            throw new IllegalArgumentException("Unsupported crypto symbol: " + symbol);
        }

        String cacheKey = symbol + ":" + period.label();
        Cache cache = cacheManager.getCache(CRYPTO_CHART_CACHE);
        if (cache != null) {
            CryptoChartImage cachedImage = cache.get(cacheKey, CryptoChartImage.class);
            if (cachedImage != null) {
                return new CryptoChartImage(
                        cachedImage.imageUrl(),
                        cachedImage.caption() + "\nNguon: cache 60 giay"
                );
            }
        }

        Instant to = Instant.now();
        Instant from = to.minus(period.duration());
        List<CryptoChartPoint> points = coinGeckoClient.getMarketChartRange(coinId.get(), from, to);
        List<CryptoChartPoint> sampledPoints = samplePoints(points);

        CryptoChartImage chartImage = new CryptoChartImage(
                buildQuickChartUrl(symbol, period, sampledPoints),
                "%s chart %s\nNguon: CoinGecko + QuickChart".formatted(symbol, period.label())
        );

        if (cache != null) {
            cache.put(cacheKey, chartImage);
        }

        return chartImage;
    }

    private List<CryptoChartPoint> samplePoints(List<CryptoChartPoint> points) {
        if (points.size() <= MAX_CHART_POINTS) {
            return points;
        }

        List<CryptoChartPoint> sampled = new ArrayList<>();
        int lastIndex = points.size() - 1;
        for (int i = 0; i < MAX_CHART_POINTS; i++) {
            int index = Math.round((float) i * lastIndex / (MAX_CHART_POINTS - 1));
            sampled.add(points.get(index));
        }

        return sampled;
    }

    private String buildQuickChartUrl(String symbol, ChartPeriod period, List<CryptoChartPoint> points) {
        List<String> labels = points.stream()
                .map(point -> formatLabel(point.time(), period))
                .toList();
        List<BigDecimal> prices = points.stream()
                .map(point -> point.priceUsd().setScale(2, RoundingMode.HALF_UP))
                .toList();

        Map<String, Object> chartConfig = Map.of(
                "type", "line",
                "data", Map.of(
                        "labels", labels,
                        "datasets", List.of(Map.of(
                                "label", symbol + " USD",
                                "data", prices,
                                "borderColor", "#2563eb",
                                "backgroundColor", "rgba(37, 99, 235, 0.12)",
                                "fill", true,
                                "tension", 0.25,
                                "pointRadius", 0
                        ))
                ),
                "options", Map.of(
                        "plugins", Map.of(
                                "title", Map.of(
                                        "display", true,
                                        "text", symbol + " price - " + period.label()
                                ),
                                "legend", Map.of("display", false)
                        )
                )
        );

        try {
            String chartJson = objectMapper.writeValueAsString(chartConfig);
            return "https://quickchart.io/chart?width=800&height=450&version=4&c="
                    + URLEncoder.encode(chartJson, StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot build chart image URL", exception);
        }
    }

    private String formatLabel(Instant time, ChartPeriod period) {
        if (period.duration().compareTo(Duration.ofHours(24)) <= 0) {
            return SHORT_TIME_FORMATTER.format(time);
        }

        return SHORT_DATE_FORMATTER.format(time);
    }

    private record ChartPeriod(String label, Duration duration) {
        private static ChartPeriod parse(String rawPeriod) {
            String period = rawPeriod == null ? "" : rawPeriod.trim().toLowerCase();
            return switch (period) {
                case "30m" -> new ChartPeriod("30m", Duration.ofMinutes(30));
                case "1h" -> new ChartPeriod("1h", Duration.ofHours(1));
                case "24h", "1d" -> new ChartPeriod("1d", Duration.ofDays(1));
                case "7d" -> new ChartPeriod("7d", Duration.ofDays(7));
                case "30d", "1m" -> new ChartPeriod("1m", Duration.ofDays(30));
                default -> throw new IllegalArgumentException("Unsupported chart period: " + rawPeriod);
            };
        }
    }
}
