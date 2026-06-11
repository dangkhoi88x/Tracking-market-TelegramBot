package com.example.trackingbot.service;

import com.example.trackingbot.client.BinanceFuturesClient;
import com.example.trackingbot.dto.BinanceKline;
import com.example.trackingbot.dto.BreakoutSignal;
import com.example.trackingbot.dto.PivotPoint;
import com.example.trackingbot.dto.TechnicalAnalysis;
import com.example.trackingbot.dto.Trendline;
import com.example.trackingbot.dto.TrendlineAnalysis;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TechnicalAnalysisService {

    private static final int KLINE_LIMIT = 160;
    private static final int RECENT_LEVEL_WINDOW = 36;
    private static final int AVERAGE_VOLUME_WINDOW = 20;
    private static final int PIVOT_LEFT = 3;
    private static final int PIVOT_RIGHT = 3;
    private static final BigDecimal TRENDLINE_TOUCH_TOLERANCE = BigDecimal.valueOf(0.003);
    private static final BigDecimal BREAKOUT_VOLUME_MULTIPLIER = BigDecimal.valueOf(1.5);

    private final BinanceFuturesClient binanceFuturesClient;

    public TechnicalAnalysisService(BinanceFuturesClient binanceFuturesClient) {
        this.binanceFuturesClient = binanceFuturesClient;
    }

    public TechnicalAnalysis analyze(String symbol, String interval) {
        String pair = symbol + "USDT";
        List<BinanceKline> klines = binanceFuturesClient.getKlines(pair, interval, KLINE_LIMIT);
        BigDecimal lastClose = lastKline(klines).close();
        List<BigDecimal> ema20 = calculateEma(klines, 20);
        List<BigDecimal> ema50 = calculateEma(klines, 50);
        BigDecimal support = findSupport(recentKlines(klines, RECENT_LEVEL_WINDOW));
        BigDecimal resistance = findResistance(recentKlines(klines, RECENT_LEVEL_WINDOW));
        List<BinanceKline> previousKlines = klines.subList(0, Math.max(1, klines.size() - 1));
        BigDecimal priorSupport = findSupport(recentKlines(previousKlines, RECENT_LEVEL_WINDOW));
        BigDecimal priorResistance = findResistance(recentKlines(previousKlines, RECENT_LEVEL_WINDOW));
        BigDecimal averageVolume20 = calculateAverageVolume(previousKlines, AVERAGE_VOLUME_WINDOW);
        BigDecimal rsi14 = calculateRsi(klines, 14);
        String bias = detectBias(lastClose, lastValue(ema20), lastValue(ema50));
        BreakoutSignal breakoutSignal = detectBreakout(klines, priorSupport, priorResistance, averageVolume20);
        TrendlineAnalysis trendlineAnalysis = analyzeTrendlines(klines);

        return new TechnicalAnalysis(
                symbol,
                pair,
                interval,
                klines,
                ema20,
                ema50,
                calculateCumulativeVolumeDelta(klines),
                lastClose,
                support,
                resistance,
                priorSupport,
                priorResistance,
                averageVolume20,
                rsi14,
                lastKline(klines).takerBuyVolume(),
                lastKline(klines).takerSellVolume(),
                lastKline(klines).volumeDelta(),
                calculateTotalVolumeDelta(klines),
                bias,
                breakoutSignal,
                trendlineAnalysis
        );
    }

    private TrendlineAnalysis analyzeTrendlines(List<BinanceKline> klines) {
        List<PivotPoint> pivotHighs = findPivotHighs(klines);
        List<PivotPoint> pivotLows = findPivotLows(klines);
        Trendline uptrend = buildTrendline("Uptrend", pivotLows, klines);
        Trendline downtrend = buildTrendline("Downtrend", pivotHighs, klines);
        String summary = buildTrendlineSummary(uptrend, downtrend);

        return new TrendlineAnalysis(
                pivotHighs,
                pivotLows,
                uptrend,
                downtrend,
                summary
        );
    }

    private List<PivotPoint> findPivotHighs(List<BinanceKline> klines) {
        List<PivotPoint> pivots = new ArrayList<>();
        for (int index = PIVOT_LEFT; index < klines.size() - PIVOT_RIGHT; index++) {
            BigDecimal price = klines.get(index).high();
            boolean isPivot = true;
            for (int offset = index - PIVOT_LEFT; offset <= index + PIVOT_RIGHT; offset++) {
                if (offset != index && klines.get(offset).high().compareTo(price) >= 0) {
                    isPivot = false;
                    break;
                }
            }

            if (isPivot) {
                pivots.add(new PivotPoint(
                        index,
                        klines.get(index).openTime().getEpochSecond(),
                        price,
                        "HIGH"
                ));
            }
        }

        return pivots;
    }

    private List<PivotPoint> findPivotLows(List<BinanceKline> klines) {
        List<PivotPoint> pivots = new ArrayList<>();
        for (int index = PIVOT_LEFT; index < klines.size() - PIVOT_RIGHT; index++) {
            BigDecimal price = klines.get(index).low();
            boolean isPivot = true;
            for (int offset = index - PIVOT_LEFT; offset <= index + PIVOT_RIGHT; offset++) {
                if (offset != index && klines.get(offset).low().compareTo(price) <= 0) {
                    isPivot = false;
                    break;
                }
            }

            if (isPivot) {
                pivots.add(new PivotPoint(
                        index,
                        klines.get(index).openTime().getEpochSecond(),
                        price,
                        "LOW"
                ));
            }
        }

        return pivots;
    }

    private Trendline buildTrendline(String type, List<PivotPoint> pivots, List<BinanceKline> klines) {
        if (pivots.size() < 2) {
            return null;
        }

        PivotPoint first = pivots.get(pivots.size() - 2);
        PivotPoint second = pivots.get(pivots.size() - 1);
        int lastIndex = klines.size() - 1;
        BigDecimal extendedPrice = priceOnLine(first, second, lastIndex);
        int touches = countTrendlineTouches(first, second, klines);
        long extendedTime = lastKline(klines).openTime().getEpochSecond();
        boolean active = type.equals("Uptrend")
                ? lastKline(klines).close().compareTo(extendedPrice) >= 0
                : lastKline(klines).close().compareTo(extendedPrice) <= 0;

        return new Trendline(
                type,
                first,
                second,
                extendedTime,
                extendedPrice,
                touches,
                active
        );
    }

    private int countTrendlineTouches(PivotPoint first, PivotPoint second, List<BinanceKline> klines) {
        int touches = 0;
        int fromIndex = first.index();
        for (int index = fromIndex; index < klines.size(); index++) {
            BigDecimal expected = priceOnLine(first, second, index);
            BigDecimal candleReference = first.type().equals("LOW")
                    ? klines.get(index).low()
                    : klines.get(index).high();
            BigDecimal tolerance = expected.abs().multiply(TRENDLINE_TOUCH_TOLERANCE);
            if (candleReference.subtract(expected).abs().compareTo(tolerance) <= 0) {
                touches++;
            }
        }

        return touches;
    }

    private BigDecimal priceOnLine(PivotPoint first, PivotPoint second, int targetIndex) {
        if (second.index() == first.index()) {
            return second.price();
        }

        BigDecimal priceDiff = second.price().subtract(first.price());
        BigDecimal indexDiff = BigDecimal.valueOf(second.index() - first.index());
        BigDecimal slope = priceDiff.divide(indexDiff, 12, RoundingMode.HALF_UP);
        return first.price()
                .add(slope.multiply(BigDecimal.valueOf(targetIndex - first.index())))
                .setScale(6, RoundingMode.HALF_UP);
    }

    private String buildTrendlineSummary(Trendline uptrend, Trendline downtrend) {
        if (uptrend == null && downtrend == null) {
            return "Not enough pivots";
        }

        if (uptrend != null && downtrend != null) {
            if (uptrend.active() && uptrend.touches() >= downtrend.touches()) {
                return "Uptrend line is active";
            }
            if (downtrend.active()) {
                return "Downtrend line is active";
            }
        }

        if (uptrend != null && uptrend.active()) {
            return "Uptrend line is active";
        }

        if (downtrend != null && downtrend.active()) {
            return "Downtrend line is active";
        }

        return "Price is between trendlines";
    }

    private List<BigDecimal> calculateEma(List<BinanceKline> klines, int period) {
        BigDecimal multiplier = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(period + 1L), 12, RoundingMode.HALF_UP);
        BigDecimal ema = klines.get(0).close();

        List<BigDecimal> values = new ArrayList<>();
        for (BinanceKline kline : klines) {
            ema = kline.close()
                    .subtract(ema)
                    .multiply(multiplier)
                    .add(ema);
            values.add(ema.setScale(6, RoundingMode.HALF_UP));
        }

        return values;
    }

    private BigDecimal calculateRsi(List<BinanceKline> klines, int period) {
        if (klines.size() <= period) {
            return BigDecimal.ZERO;
        }

        BigDecimal averageGain = BigDecimal.ZERO;
        BigDecimal averageLoss = BigDecimal.ZERO;

        for (int index = 1; index <= period; index++) {
            BigDecimal change = klines.get(index).close().subtract(klines.get(index - 1).close());
            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                averageGain = averageGain.add(change);
            } else {
                averageLoss = averageLoss.add(change.abs());
            }
        }

        averageGain = averageGain.divide(BigDecimal.valueOf(period), 12, RoundingMode.HALF_UP);
        averageLoss = averageLoss.divide(BigDecimal.valueOf(period), 12, RoundingMode.HALF_UP);

        for (int index = period + 1; index < klines.size(); index++) {
            BigDecimal change = klines.get(index).close().subtract(klines.get(index - 1).close());
            BigDecimal gain = change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO;
            averageGain = averageGain.multiply(BigDecimal.valueOf(period - 1L))
                    .add(gain)
                    .divide(BigDecimal.valueOf(period), 12, RoundingMode.HALF_UP);
            averageLoss = averageLoss.multiply(BigDecimal.valueOf(period - 1L))
                    .add(loss)
                    .divide(BigDecimal.valueOf(period), 12, RoundingMode.HALF_UP);
        }

        if (averageLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal relativeStrength = averageGain.divide(averageLoss, 12, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(100)
                        .divide(BigDecimal.ONE.add(relativeStrength), 6, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> calculateCumulativeVolumeDelta(List<BinanceKline> klines) {
        BigDecimal cumulative = BigDecimal.ZERO;
        List<BigDecimal> values = new ArrayList<>();
        for (BinanceKline kline : klines) {
            cumulative = cumulative.add(kline.volumeDelta());
            values.add(cumulative.setScale(6, RoundingMode.HALF_UP));
        }

        return values;
    }

    private BigDecimal calculateTotalVolumeDelta(List<BinanceKline> klines) {
        return klines.stream()
                .map(BinanceKline::volumeDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageVolume(List<BinanceKline> klines, int window) {
        List<BinanceKline> recent = recentKlines(klines, window);
        BigDecimal total = recent.stream()
                .map(BinanceKline::volume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(recent.size()), 8, RoundingMode.HALF_UP);
    }

    private BreakoutSignal detectBreakout(
            List<BinanceKline> klines,
            BigDecimal priorSupport,
            BigDecimal priorResistance,
            BigDecimal averageVolume20
    ) {
        BinanceKline last = lastKline(klines);
        boolean volumeSpike = last.volume().compareTo(averageVolume20.multiply(BREAKOUT_VOLUME_MULTIPLIER)) >= 0;
        boolean positiveDelta = last.volumeDelta().compareTo(BigDecimal.ZERO) > 0;
        boolean negativeDelta = last.volumeDelta().compareTo(BigDecimal.ZERO) < 0;
        BigDecimal volumeRatio = averageVolume20.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : last.volume().divide(averageVolume20, 4, RoundingMode.HALF_UP);

        if (last.close().compareTo(priorResistance) > 0) {
            boolean confirmed = volumeSpike && positiveDelta;
            return new BreakoutSignal(
                    "Bullish Breakout",
                    confirmed,
                    priorResistance,
                    last.close(),
                    averageVolume20,
                    last.volume(),
                    volumeRatio,
                    last.volumeDelta(),
                    buildBreakoutReasons(
                            "Close above resistance",
                            confirmed,
                            volumeSpike,
                            positiveDelta
                    )
            );
        }

        if (last.close().compareTo(priorSupport) < 0) {
            boolean confirmed = volumeSpike && negativeDelta;
            return new BreakoutSignal(
                    "Bearish Breakdown",
                    confirmed,
                    priorSupport,
                    last.close(),
                    averageVolume20,
                    last.volume(),
                    volumeRatio,
                    last.volumeDelta(),
                    buildBreakoutReasons(
                            "Close below support",
                            confirmed,
                            volumeSpike,
                            negativeDelta
                    )
            );
        }

        return new BreakoutSignal(
                "No Breakout",
                false,
                last.close().compareTo(midpoint(priorSupport, priorResistance)) >= 0 ? priorResistance : priorSupport,
                last.close(),
                averageVolume20,
                last.volume(),
                volumeRatio,
                last.volumeDelta(),
                List.of("Price is still inside support/resistance range")
        );
    }

    private List<String> buildBreakoutReasons(
            String priceReason,
            boolean confirmed,
            boolean volumeSpike,
            boolean deltaAligned
    ) {
        return List.of(
                priceReason,
                volumeSpike ? "Volume above 20-candle average x1.5" : "Volume is not strong enough",
                deltaAligned ? "Volume delta supports the move" : "Volume delta does not support the move",
                confirmed ? "Breakout confirmed" : "Breakout not confirmed"
        );
    }

    private String detectBias(BigDecimal lastClose, BigDecimal ema20, BigDecimal ema50) {
        if (lastClose.compareTo(ema20) > 0 && ema20.compareTo(ema50) > 0) {
            return "Bullish";
        }

        if (lastClose.compareTo(ema20) < 0 && ema20.compareTo(ema50) < 0) {
            return "Bearish";
        }

        return "Neutral";
    }

    private BigDecimal findSupport(List<BinanceKline> klines) {
        return klines.stream()
                .map(BinanceKline::low)
                .min(Comparator.naturalOrder())
                .orElse(lastKline(klines).low());
    }

    private BigDecimal findResistance(List<BinanceKline> klines) {
        return klines.stream()
                .map(BinanceKline::high)
                .max(Comparator.naturalOrder())
                .orElse(lastKline(klines).high());
    }

    private List<BinanceKline> recentKlines(List<BinanceKline> klines, int window) {
        int fromIndex = Math.max(0, klines.size() - window);
        return klines.subList(fromIndex, klines.size());
    }

    private BigDecimal midpoint(BigDecimal support, BigDecimal resistance) {
        return support.add(resistance).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal lastValue(List<BigDecimal> values) {
        return values.get(values.size() - 1);
    }

    private BinanceKline lastKline(List<BinanceKline> klines) {
        return klines.get(klines.size() - 1);
    }
}
