package com.example.trackingbot.service.analysis;

import com.example.trackingbot.client.OpenAiClient;
import com.example.trackingbot.config.IdeaChartProperties;
import com.example.trackingbot.dto.response.AiPredictionResponse;
import com.example.trackingbot.model.BinanceKline;
import com.example.trackingbot.dto.response.IdeaChartImage;
import com.example.trackingbot.model.OrderFlowAnalysis;
import com.example.trackingbot.model.TechnicalAnalysis;
import com.example.trackingbot.model.Trendline;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AiPredictionService {

    private static final String AI_PREDICTION_CACHE = "ai-prediction";
    private static final String DEFAULT_INTERVAL = "4h";

    private final CryptoPriceService cryptoPriceService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final OrderFlowService orderFlowService;
    private final OpenAiClient openAiClient;
    private final IdeaChartProperties ideaChartProperties;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getHelpMessage() {
        return """
                Cach dung:
                /ai BTC
                /ai ETH 1h
                /ai SOL 1d
                /ai_chart BTC

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

    public IdeaChartImage createAiChart(String rawArguments) {
        AiCommand command = parseCommand(rawArguments);
        TechnicalAnalysis technical = technicalAnalysisService.analyze(command.symbol(), command.interval());
        OrderFlowAnalysis orderFlow = orderFlowService.analyze(command.symbol());
        AiPredictionResponse prediction = openAiClient.analyze(buildMarketData(technical, orderFlow));

        Path outputDir = Path.of(ideaChartProperties.outputDirOrDefault());
        createDirectories(outputDir);
        Path inputJson = outputDir.resolve("ai-chart-" + UUID.randomUUID() + ".json");
        Path outputPng = outputDir.resolve("ai-chart-" + UUID.randomUUID() + ".png");

        writeAiChartPayload(inputJson, technical, orderFlow, prediction);
        runRenderer(inputJson, outputPng);

        return new IdeaChartImage(
                outputPng,
                "%s %s AI Quant Map | Bias: %s | Confidence: %s%% | Risk: %s".formatted(
                        technical.pair(),
                        technical.interval(),
                        prediction.bias(),
                        prediction.confidence(),
                        prediction.riskLevel()
                ),
                technical.symbol(),
                technical.interval()
        );
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

    private void writeAiChartPayload(
            Path inputJson,
            TechnicalAnalysis technical,
            OrderFlowAnalysis orderFlow,
            AiPredictionResponse prediction
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chartType", "AI_ANALYSIS");
        payload.put("symbol", technical.pair());
        payload.put("interval", technical.interval());
        payload.put("createdAt", Instant.now().toString());
        payload.put("title", "AI Quant Map");
        payload.put("badge", prediction.bias() + " " + prediction.confidence() + "%");
        payload.put("note", "AI scenarios are generated from backend technical data. Not financial advice.");
        payload.put("candles", toCandles(technical.klines()));
        payload.put("ema20", toLinePoints(technical.klines(), technical.ema20()));
        payload.put("ema50", toLinePoints(technical.klines(), technical.ema50()));
        payload.put("idea", Map.of(
                "bias", prediction.bias(),
                "support", technical.support(),
                "resistance", technical.resistance()
        ));
        payload.put("orderFlow", Map.of(
                "bidDominance", orderFlow.bidDominance(),
                "tradeBuyRatio", orderFlow.tradeBuyRatio(),
                "tradePressure", orderFlow.tradePressure(),
                "fundingRate", orderFlow.fundingRate()
        ));
        payload.put("aiAnalysis", Map.of(
                "bias", prediction.bias(),
                "confidence", prediction.confidence(),
                "riskLevel", prediction.riskLevel(),
                "marketRegime", prediction.marketRegime(),
                "bullishScenario", prediction.bullishScenario(),
                "bearishScenario", prediction.bearishScenario(),
                "invalidation", prediction.invalidation(),
                "chartAnnotations", prediction.chartAnnotations()
        ));

        try {
            objectMapper.writeValue(inputJson.toFile(), payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write AI chart payload", exception);
        }
    }

    private List<Map<String, Object>> toCandles(List<BinanceKline> klines) {
        return klines.stream()
                .map(kline -> Map.<String, Object>of(
                        "time", kline.openTime().getEpochSecond(),
                        "open", kline.open(),
                        "high", kline.high(),
                        "low", kline.low(),
                        "close", kline.close(),
                        "volume", kline.volume()
                ))
                .toList();
    }

    private List<Map<String, Object>> toLinePoints(List<BinanceKline> klines, List<BigDecimal> values) {
        return IntStream.range(0, klines.size())
                .mapToObj(index -> Map.<String, Object>of(
                        "time", klines.get(index).openTime().getEpochSecond(),
                        "value", values.get(index)
                ))
                .toList();
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

    private void runRenderer(Path inputJson, Path outputPng) {
        Path script = Path.of(ideaChartProperties.rendererScriptOrDefault());
        ProcessBuilder processBuilder = new ProcessBuilder(
                ideaChartProperties.nodeBinOrDefault(),
                script.toString(),
                inputJson.toString(),
                outputPng.toString()
        );
        processBuilder.directory(Path.of("").toAbsolutePath().toFile());

        if (ideaChartProperties.nodePath() != null && !ideaChartProperties.nodePath().isBlank()) {
            processBuilder.environment().put("NODE_PATH", ideaChartProperties.nodePath());
        }

        try {
            Process process = processBuilder.start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("AI chart renderer failed: " + stderr + stdout);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot run AI chart renderer", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI chart renderer was interrupted", exception);
        }
    }

    private void createDirectories(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create AI chart output directory", exception);
        }
    }

    private record AiCommand(
            String symbol,
            String interval
    ) {
    }
}
