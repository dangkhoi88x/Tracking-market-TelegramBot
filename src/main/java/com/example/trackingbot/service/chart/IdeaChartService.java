package com.example.trackingbot.service.chart;

import com.example.trackingbot.config.IdeaChartProperties;
import com.example.trackingbot.dto.response.BinanceKline;
import com.example.trackingbot.dto.response.IdeaChartImage;
import com.example.trackingbot.dto.response.OrderFlowAnalysis;
import com.example.trackingbot.dto.response.PivotPoint;
import com.example.trackingbot.dto.response.TechnicalAnalysis;
import com.example.trackingbot.dto.response.Trendline;
import com.example.trackingbot.service.analysis.OrderFlowService;
import com.example.trackingbot.service.analysis.TechnicalAnalysisService;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
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
public class IdeaChartService {

    private static final String DEFAULT_INTERVAL = "4h";

    private final CryptoPriceService cryptoPriceService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final OrderFlowService orderFlowService;
    private final IdeaChartProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IdeaChartService(
            CryptoPriceService cryptoPriceService,
            TechnicalAnalysisService technicalAnalysisService,
            OrderFlowService orderFlowService,
            IdeaChartProperties properties
    ) {
        this.cryptoPriceService = cryptoPriceService;
        this.technicalAnalysisService = technicalAnalysisService;
        this.orderFlowService = orderFlowService;
        this.properties = properties;
    }

    public IdeaChartImage createIdeaChart(String rawArguments) {
        return createChart(rawArguments, IdeaChartType.IDEA);
    }

    public IdeaChartImage createVolumeDeltaChart(String rawArguments) {
        return createChart(rawArguments, IdeaChartType.VOLUME_DELTA);
    }

    public IdeaChartImage createBreakoutChart(String rawArguments) {
        return createChart(rawArguments, IdeaChartType.BREAKOUT);
    }

    public IdeaChartImage createTrendlineChart(String rawArguments) {
        return createChart(rawArguments, IdeaChartType.TRENDLINE);
    }

    public IdeaChartImage createOrderFlowChart(String rawArguments) {
        return createChart(rawArguments, IdeaChartType.ORDER_FLOW);
    }

    public String getHelpMessage() {
        return """
                Cach dung:
                /idea BTC
                /idea ETH 1h
                /idea SOL 1d

                Timeframe ho tro:
                1h, 4h, 1d
                """;
    }

    public String getVolumeDeltaHelpMessage() {
        return """
                Cach dung:
                /chart_volume BTC
                /chart_volume ETH 1h
                /chart_volume SOL 1d

                Chart nay hien Volume Delta: buy volume - sell volume.
                """;
    }

    public String getBreakoutHelpMessage() {
        return """
                Cach dung:
                /chart_breakout BTC
                /chart_breakout ETH 1h
                /chart_breakout SOL 1d

                Chart nay xac nhan breakout bang close, volume va volume delta.
                """;
    }

    public String getTrendlineHelpMessage() {
        return """
                Cach dung:
                /chart_trendline BTC
                /chart_trendline ETH 1h
                /chart_trendline SOL 1d

                Chart nay tim pivot high/low voi left=3, right=3 roi ve trendline that.
                """;
    }

    public String getOrderFlowHelpMessage() {
        return """
                Cach dung:
                /chart_orderflow BTC
                /chart_orderflow ETH 1h
                /chart_orderflow SOL 1d

                Chart nay doc depth, aggTrades, openInterest va funding rate tu Binance Futures.
                """;
    }

    private IdeaChartImage createChart(String rawArguments, IdeaChartType chartType) {
        IdeaCommand command = parseIdeaCommand(rawArguments);
        TechnicalAnalysis analysis = technicalAnalysisService.analyze(command.symbol(), command.interval());
        OrderFlowAnalysis orderFlow = chartType == IdeaChartType.ORDER_FLOW
                ? orderFlowService.analyze(command.symbol())
                : null;

        Path outputDir = Path.of(properties.outputDirOrDefault());
        createDirectories(outputDir);
        Path inputJson = outputDir.resolve("idea-" + UUID.randomUUID() + ".json");
        Path outputPng = outputDir.resolve("idea-" + UUID.randomUUID() + ".png");

        writeChartPayload(inputJson, analysis, orderFlow, chartType);
        runRenderer(inputJson, outputPng);

        return new IdeaChartImage(
                outputPng,
                buildCaption(analysis, orderFlow, chartType),
                analysis.symbol(),
                analysis.interval()
        );
    }

    private IdeaCommand parseIdeaCommand(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            throw new IllegalArgumentException("Missing idea arguments");
        }

        String[] parts = rawArguments.trim().split("\\s+");
        if (parts.length < 1 || parts.length > 2) {
            throw new IllegalArgumentException("Invalid idea format");
        }

        String symbol = cryptoPriceService.normalizeSymbol(parts[0]);
        if (cryptoPriceService.findCoinId(symbol).isEmpty()) {
            throw new IllegalArgumentException("Unsupported symbol: " + symbol);
        }

        String interval = parts.length == 2 ? parts[1].toLowerCase(Locale.ROOT) : DEFAULT_INTERVAL;
        if (!List.of("1h", "4h", "1d").contains(interval)) {
            throw new IllegalArgumentException("Unsupported idea interval: " + interval);
        }

        return new IdeaCommand(symbol, interval);
    }

    private void writeChartPayload(
            Path inputJson,
            TechnicalAnalysis analysis,
            OrderFlowAnalysis orderFlow,
            IdeaChartType chartType
    ) {
        List<Map<String, Object>> candles = analysis.klines().stream()
                .map(kline -> Map.<String, Object>of(
                        "time", kline.openTime().getEpochSecond(),
                        "open", kline.open(),
                        "high", kline.high(),
                        "low", kline.low(),
                        "close", kline.close(),
                        "volume", kline.volume(),
                        "buyVolume", kline.takerBuyVolume(),
                        "sellVolume", kline.takerSellVolume(),
                        "volumeDelta", kline.volumeDelta()
                ))
                .toList();

        Map<String, Object> breakout = new LinkedHashMap<>();
        breakout.put("direction", analysis.breakoutSignal().direction());
        breakout.put("confirmed", analysis.breakoutSignal().confirmed());
        breakout.put("support", analysis.priorSupport());
        breakout.put("resistance", analysis.priorResistance());
        breakout.put("referenceLevel", analysis.breakoutSignal().referenceLevel());
        breakout.put("close", analysis.breakoutSignal().close());
        breakout.put("averageVolume", analysis.breakoutSignal().averageVolume());
        breakout.put("lastVolume", analysis.breakoutSignal().lastVolume());
        breakout.put("volumeRatio", analysis.breakoutSignal().volumeRatio());
        breakout.put("volumeDelta", analysis.breakoutSignal().volumeDelta());
        breakout.put("reasons", analysis.breakoutSignal().reasons());

        Map<String, Object> trendline = new LinkedHashMap<>();
        trendline.put("summary", analysis.trendlineAnalysis().summary());
        trendline.put("pivotHighs", toPivotPoints(analysis.trendlineAnalysis().pivotHighs()));
        trendline.put("pivotLows", toPivotPoints(analysis.trendlineAnalysis().pivotLows()));
        trendline.put("uptrend", toTrendline(analysis.trendlineAnalysis().uptrend()));
        trendline.put("downtrend", toTrendline(analysis.trendlineAnalysis().downtrend()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chartType", chartType.payloadName());
        payload.put("symbol", analysis.pair());
        payload.put("interval", analysis.interval());
        payload.put("createdAt", Instant.now().toString());
        payload.put("title", chartType.title());
        payload.put("badge", buildBadge(analysis, chartType));
        payload.put("note", chartType.note());
        payload.put("candles", candles);
        payload.put("ema20", toLinePoints(analysis.klines(), analysis.ema20()));
        payload.put("ema50", toLinePoints(analysis.klines(), analysis.ema50()));
        payload.put("idea", Map.of(
                "bias", analysis.bias(),
                "support", analysis.support(),
                "resistance", analysis.resistance()
        ));
        payload.put("volumeDelta", Map.of(
                "lastBuyVolume", analysis.lastBuyVolume(),
                "lastSellVolume", analysis.lastSellVolume(),
                "lastDelta", analysis.lastVolumeDelta(),
                "totalDelta", analysis.totalVolumeDelta(),
                "points", toVolumeDeltaPoints(analysis),
                "cumulative", toLinePoints(analysis.klines(), analysis.cumulativeVolumeDelta())
        ));
        payload.put("breakout", breakout);
        payload.put("trendline", trendline);
        payload.put("orderFlow", toOrderFlow(orderFlow));

        try {
            objectMapper.writeValue(inputJson.toFile(), payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write idea chart payload", exception);
        }
    }

    private List<Map<String, Object>> toLinePoints(List<BinanceKline> klines, List<BigDecimal> values) {
        return IntStream.range(0, klines.size())
                .mapToObj(index -> Map.<String, Object>of(
                        "time", klines.get(index).openTime().getEpochSecond(),
                        "value", values.get(index)
                ))
                .toList();
    }

    private List<Map<String, Object>> toVolumeDeltaPoints(TechnicalAnalysis analysis) {
        return analysis.klines().stream()
                .map(kline -> Map.<String, Object>of(
                        "time", kline.openTime().getEpochSecond(),
                        "value", kline.volumeDelta(),
                        "color", kline.volumeDelta().compareTo(BigDecimal.ZERO) >= 0
                                ? "rgba(16, 185, 129, 0.55)"
                                : "rgba(239, 68, 68, 0.55)"
                ))
                .toList();
    }

    private List<Map<String, Object>> toPivotPoints(List<PivotPoint> pivots) {
        return pivots.stream()
                .map(pivot -> Map.<String, Object>of(
                        "index", pivot.index(),
                        "time", pivot.time(),
                        "price", pivot.price(),
                        "type", pivot.type()
                ))
                .toList();
    }

    private Map<String, Object> toTrendline(Trendline trendline) {
        if (trendline == null) {
            return Map.of("active", false);
        }

        return Map.of(
                "type", trendline.type(),
                "first", Map.of(
                        "index", trendline.first().index(),
                        "time", trendline.first().time(),
                        "price", trendline.first().price()
                ),
                "second", Map.of(
                        "index", trendline.second().index(),
                        "time", trendline.second().time(),
                        "price", trendline.second().price()
                ),
                "extendedTime", trendline.extendedTime(),
                "extendedPrice", trendline.extendedPrice(),
                "touches", trendline.touches(),
                "active", trendline.active()
        );
    }

    private Map<String, Object> toOrderFlow(OrderFlowAnalysis orderFlow) {
        if (orderFlow == null) {
            return Map.of();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bidVolume", orderFlow.bidVolume());
        payload.put("askVolume", orderFlow.askVolume());
        payload.put("bidDominance", orderFlow.bidDominance());
        payload.put("buyTradeVolume", orderFlow.buyTradeVolume());
        payload.put("sellTradeVolume", orderFlow.sellTradeVolume());
        payload.put("tradeBuyRatio", orderFlow.tradeBuyRatio());
        payload.put("openInterest", orderFlow.openInterest());
        payload.put("fundingRate", orderFlow.fundingRate());
        payload.put("bookPressure", orderFlow.bookPressure());
        payload.put("tradePressure", orderFlow.tradePressure());
        payload.put("openInterestState", orderFlow.openInterestState());
        payload.put("summary", orderFlow.summary());
        return payload;
    }

    private String buildCaption(TechnicalAnalysis analysis, OrderFlowAnalysis orderFlow, IdeaChartType chartType) {
        return switch (chartType) {
            case IDEA -> "%s %s idea | Bias: %s | Support: %s | Resistance: %s".formatted(
                    analysis.pair(),
                    analysis.interval(),
                    analysis.bias(),
                    formatMoney(analysis.support()),
                    formatMoney(analysis.resistance())
            );
            case VOLUME_DELTA -> "%s %s volume delta | Last: %s | Total: %s".formatted(
                    analysis.pair(),
                    analysis.interval(),
                    formatNumber(analysis.lastVolumeDelta()),
                    formatNumber(analysis.totalVolumeDelta())
            );
            case BREAKOUT -> "%s %s breakout | %s | Confirmed: %s | Vol x%s".formatted(
                    analysis.pair(),
                    analysis.interval(),
                    analysis.breakoutSignal().direction(),
                    analysis.breakoutSignal().confirmed() ? "YES" : "NO",
                    analysis.breakoutSignal().volumeRatio().setScale(2, java.math.RoundingMode.HALF_UP)
            );
            case TRENDLINE -> "%s %s trendline | %s | Up touches: %s | Down touches: %s".formatted(
                    analysis.pair(),
                    analysis.interval(),
                    analysis.trendlineAnalysis().summary(),
                    trendlineTouches(analysis.trendlineAnalysis().uptrend()),
                    trendlineTouches(analysis.trendlineAnalysis().downtrend())
            );
            case ORDER_FLOW -> "%s %s order flow | Bid dominance: %s%% | Trades: %s".formatted(
                    analysis.pair(),
                    analysis.interval(),
                    orderFlow.bidDominance().multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP),
                    orderFlow.tradePressure()
            );
        };
    }

    private String buildBadge(TechnicalAnalysis analysis, IdeaChartType chartType) {
        return switch (chartType) {
            case IDEA -> "Bias: " + analysis.bias();
            case VOLUME_DELTA -> "Delta: " + formatNumber(analysis.lastVolumeDelta());
            case BREAKOUT -> analysis.breakoutSignal().confirmed()
                    ? "Breakout Confirmed"
                    : analysis.breakoutSignal().direction();
            case TRENDLINE -> analysis.trendlineAnalysis().summary();
            case ORDER_FLOW -> "Order Flow";
        };
    }

    private int trendlineTouches(Trendline trendline) {
        return trendline == null ? 0 : trendline.touches();
    }

    private void runRenderer(Path inputJson, Path outputPng) {
        Path script = Path.of(properties.rendererScriptOrDefault());
        ProcessBuilder processBuilder = new ProcessBuilder(
                properties.nodeBinOrDefault(),
                script.toString(),
                inputJson.toString(),
                outputPng.toString()
        );
        processBuilder.directory(Path.of("").toAbsolutePath().toFile());

        if (properties.nodePath() != null && !properties.nodePath().isBlank()) {
            processBuilder.environment().put("NODE_PATH", properties.nodePath());
        }

        try {
            Process process = processBuilder.start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Idea chart renderer failed: " + stderr + stdout);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot run idea chart renderer", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Idea chart renderer was interrupted", exception);
        }
    }

    private void createDirectories(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create idea chart output directory", exception);
        }
    }

    private String formatMoney(BigDecimal value) {
        return "%,.2f".formatted(value);
    }

    private String formatNumber(BigDecimal value) {
        return "%,.2f".formatted(value);
    }

    private enum IdeaChartType {
        IDEA("IDEA", "Idea Chart", "Auto idea: EMA20/EMA50 trend + recent support/resistance. Not financial advice."),
        VOLUME_DELTA("VOLUME_DELTA", "Volume Delta", "Volume Delta = taker buy volume - taker sell volume."),
        BREAKOUT("BREAKOUT", "Breakout Confirmation", "Breakout requires close beyond level, volume spike, and aligned volume delta."),
        TRENDLINE("TRENDLINE", "Trendline", "Pivot high/low uses left=3 and right=3, then connects the latest pivots."),
        ORDER_FLOW("ORDER_FLOW", "Order Flow", "Order flow reads order book, recent trades, open interest, and funding rate.");

        private final String payloadName;
        private final String title;
        private final String note;

        IdeaChartType(String payloadName, String title, String note) {
            this.payloadName = payloadName;
            this.title = title;
            this.note = note;
        }

        private String payloadName() {
            return payloadName;
        }

        private String title() {
            return title;
        }

        private String note() {
            return note;
        }
    }

    private record IdeaCommand(
            String symbol,
            String interval
    ) {
    }
}
