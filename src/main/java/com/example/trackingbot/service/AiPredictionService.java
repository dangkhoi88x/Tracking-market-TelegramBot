package com.example.trackingbot.service;

import com.example.trackingbot.client.OpenAiClient;
import com.example.trackingbot.dto.AiPredictionResponse;
import com.example.trackingbot.dto.OrderFlowAnalysis;
import com.example.trackingbot.dto.TechnicalAnalysis;
import com.example.trackingbot.dto.Trendline;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiPredictionService {

    private static final String AI_PREDICTION_CACHE = "ai-prediction";
    private static final String DEFAULT_INTERVAL = "4h";

    private final CryptoPriceService cryptoPriceService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final OrderFlowService orderFlowService;
    private final OpenAiClient openAiClient;
    private final CacheManager cacheManager;

    public AiPredictionService(
            CryptoPriceService cryptoPriceService,
            TechnicalAnalysisService technicalAnalysisService,
            OrderFlowService orderFlowService,
            OpenAiClient openAiClient,
            CacheManager cacheManager
    ) {
        this.cryptoPriceService = cryptoPriceService;
        this.technicalAnalysisService = technicalAnalysisService;
        this.orderFlowService = orderFlowService;
        this.openAiClient = openAiClient;
        this.cacheManager = cacheManager;
    }

    public String getHelpMessage() {
        return """
                Cach dung:
                /ai BTC
                /ai ETH 1h
                /ai SOL 1d

                Bot se lay technical data truoc, sau do GPT-5 mini tong hop thanh quant market analysis.
                Can cau hinh OPENAI_API_KEY.
                """;
    }

    public String getAiPredictionMessage(String rawArguments) {
        AiCommand command = parseCommand(rawArguments);
        String cacheKey = command.symbol() + ":" + command.interval();
        Cache cache = cacheManager.getCache(AI_PREDICTION_CACHE);
        if (cache != null) {
            String cached = cache.get(cacheKey, String.class);
            if (cached != null) {
                return cached + "\n\nNguon: AI cache 60 giay";
            }
        }

        TechnicalAnalysis technical = technicalAnalysisService.analyze(command.symbol(), command.interval());
        OrderFlowAnalysis orderFlow = orderFlowService.analyze(command.symbol());
        AiPredictionResponse prediction = openAiClient.analyze(buildMarketData(technical, orderFlow));
        String message = formatMessage(technical, orderFlow, prediction);

        if (cache != null) {
            cache.put(cacheKey, message);
        }

        return message;
    }

    private AiCommand parseCommand(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            throw new IllegalArgumentException("Missing AI prediction arguments");
        }

        String[] parts = rawArguments.trim().split("\\s+");
        if (parts.length < 1 || parts.length > 2) {
            throw new IllegalArgumentException("Invalid AI prediction format");
        }

        String symbol = cryptoPriceService.normalizeSymbol(parts[0]);
        if (cryptoPriceService.findCoinId(symbol).isEmpty()) {
            throw new IllegalArgumentException("Unsupported symbol: " + symbol);
        }

        String interval = parts.length == 2 ? parts[1].toLowerCase(Locale.ROOT) : DEFAULT_INTERVAL;
        if (!List.of("1h", "4h", "1d").contains(interval)) {
            throw new IllegalArgumentException("Unsupported AI interval: " + interval);
        }

        return new AiCommand(symbol, interval);
    }

    private Map<String, Object> buildMarketData(TechnicalAnalysis technical, OrderFlowAnalysis orderFlow) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generatedAtUtc", Instant.now().toString());
        data.put("symbol", technical.pair());
        data.put("timeframe", technical.interval());
        data.put("price", technical.lastClose());
        data.put("ema20", lastValue(technical.ema20()));
        data.put("ema50", lastValue(technical.ema50()));
        data.put("rsi14", technical.rsi14());
        data.put("support", technical.support());
        data.put("resistance", technical.resistance());
        data.put("priorSupport", technical.priorSupport());
        data.put("priorResistance", technical.priorResistance());
        data.put("averageVolume20", technical.averageVolume20());
        data.put("volumeDelta", Map.of(
                "lastBuyVolume", technical.lastBuyVolume(),
                "lastSellVolume", technical.lastSellVolume(),
                "lastDelta", technical.lastVolumeDelta(),
                "totalDelta", technical.totalVolumeDelta()
        ));
        data.put("breakout", Map.of(
                "direction", technical.breakoutSignal().direction(),
                "confirmed", technical.breakoutSignal().confirmed(),
                "referenceLevel", technical.breakoutSignal().referenceLevel(),
                "volumeRatio", technical.breakoutSignal().volumeRatio(),
                "volumeDelta", technical.breakoutSignal().volumeDelta(),
                "reasons", technical.breakoutSignal().reasons()
        ));
        data.put("trendline", Map.of(
                "summary", technical.trendlineAnalysis().summary(),
                "uptrend", trendlineData(technical.trendlineAnalysis().uptrend()),
                "downtrend", trendlineData(technical.trendlineAnalysis().downtrend())
        ));
        data.put("orderFlow", Map.of(
                "bidDominance", orderFlow.bidDominance(),
                "bookPressure", orderFlow.bookPressure(),
                "tradeBuyRatio", orderFlow.tradeBuyRatio(),
                "tradePressure", orderFlow.tradePressure(),
                "openInterest", orderFlow.openInterest(),
                "openInterestState", orderFlow.openInterestState(),
                "fundingRate", orderFlow.fundingRate(),
                "summary", orderFlow.summary()
        ));

        return data;
    }

    private Map<String, Object> trendlineData(Trendline trendline) {
        if (trendline == null) {
            return Map.of("available", false);
        }

        return Map.of(
                "available", true,
                "active", trendline.active(),
                "touches", trendline.touches(),
                "extendedPrice", trendline.extendedPrice()
        );
    }

    private String formatMessage(
            TechnicalAnalysis technical,
            OrderFlowAnalysis orderFlow,
            AiPredictionResponse prediction
    ) {
        return """
                %s AI Quant Market Analysis
                Timeframe: %s

                Bias: %s
                Confidence: %s%%
                Risk: %s
                Regime: %s

                Market Context:
                %s

                Quant Evidence:
                %s

                Levels:
                Price: %s
                EMA20 / EMA50: %s / %s
                RSI14: %s
                Support / Resistance: %s / %s
                %s

                Scenarios:
                Bullish: %s
                Bearish: %s

                Invalidation:
                %s

                Risk Management:
                %s

                Watch Triggers:
                %s

                Order Flow:
                Bid dominance: %s%%
                Trades: %s
                Funding: %s%%

                Not financial advice.
                """.formatted(
                technical.pair(),
                technical.interval(),
                prediction.bias(),
                prediction.confidence(),
                prediction.riskLevel(),
                prediction.marketRegime(),
                prediction.executiveSummary(),
                formatBulletList(prediction.evidence()),
                formatMoney(technical.lastClose()),
                formatMoney(lastValue(technical.ema20())),
                formatMoney(lastValue(technical.ema50())),
                technical.rsi14(),
                formatMoney(technical.support()),
                formatMoney(technical.resistance()),
                prediction.keyLevels(),
                prediction.bullishScenario(),
                prediction.bearishScenario(),
                prediction.invalidation(),
                prediction.riskManagement(),
                formatBulletList(prediction.watchlistTriggers()),
                percent(orderFlow.bidDominance()),
                orderFlow.tradePressure(),
                percent(orderFlow.fundingRate())
        ).trim();
    }

    private String formatBulletList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- n/a";
        }

        return items.stream()
                .map(item -> "- " + item)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- n/a");
    }

    private BigDecimal lastValue(List<BigDecimal> values) {
        return values.get(values.size() - 1);
    }

    private String formatMoney(BigDecimal value) {
        return "%,.2f".formatted(value);
    }

    private String percent(BigDecimal ratio) {
        return ratio.multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private record AiCommand(
            String symbol,
            String interval
    ) {
    }
}
