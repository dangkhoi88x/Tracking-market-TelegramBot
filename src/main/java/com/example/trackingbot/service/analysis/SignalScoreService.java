package com.example.trackingbot.service.analysis;

import com.example.trackingbot.dto.response.BreakoutSignal;
import com.example.trackingbot.dto.response.OrderFlowAnalysis;
import com.example.trackingbot.dto.response.SignalScore;
import com.example.trackingbot.dto.response.TechnicalAnalysis;
import com.example.trackingbot.dto.response.Trendline;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class SignalScoreService {

    private static final int BASE_SCORE = 50;
    private static final String DEFAULT_INTERVAL = "4h";

    private final TechnicalAnalysisService technicalAnalysisService;
    private final OrderFlowService orderFlowService;
    private final CryptoPriceService cryptoPriceService;

    public String getHelpMessage() {
        return """
                Cach dung:
                /signal BTC
                /signal ETH 1h

                Bot se tong hop RSI, EMA, Volume Delta, Breakout, Trendline va Order Flow thanh diem 0-100.
                """;
    }

    public String getSignalMessage(String arguments) {
        SignalCommand command = parseCommand(arguments);
        TechnicalAnalysis technical = technicalAnalysisService.analyze(command.symbol(), command.interval());
        OrderFlowAnalysis orderFlow = orderFlowService.analyze(command.symbol());
        SignalScore score = score(technical, orderFlow);

        return """
                %s Signal Score: %d/100
                Bias: %s
                Risk: %s
                Timeframe: %s

                Trend: %+d
                Momentum: %+d
                Order flow: %+d
                Volume: %+d
                Breakout: %+d
                Trendline: %+d

                Context:
                Price: %s
                EMA20 / EMA50: %s / %s
                RSI14: %s
                Support / Resistance: %s / %s
                Breakout: %s
                Order Flow: %s, %s

                Summary:
                %s
                """.formatted(
                score.symbol(),
                score.totalScore(),
                score.bias(),
                score.risk(),
                score.interval(),
                score.trendScore(),
                score.momentumScore(),
                score.orderFlowScore(),
                score.volumeScore(),
                score.breakoutScore(),
                score.trendlineScore(),
                formatMoney(technical.lastClose()),
                formatMoney(lastValue(technical.ema20())),
                formatMoney(lastValue(technical.ema50())),
                formatPlain(technical.rsi14()),
                formatMoney(technical.support()),
                formatMoney(technical.resistance()),
                technical.breakoutSignal().direction(),
                orderFlow.bookPressure(),
                orderFlow.tradePressure(),
                score.summary()
        );
    }

    private SignalCommand parseCommand(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            throw new IllegalArgumentException("Signal symbol is required");
        }

        String[] parts = arguments.trim().split("\\s+");
        String symbol = cryptoPriceService.normalizeSymbol(parts[0]);
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Signal symbol is required");
        }

        String interval = parts.length >= 2 ? parts[1].trim() : DEFAULT_INTERVAL;
        if (!interval.matches("^[0-9]+[mhdwM]$")) {
            throw new IllegalArgumentException("Unsupported interval: " + interval);
        }

        return new SignalCommand(symbol, interval);
    }

    private SignalScore score(TechnicalAnalysis technical, OrderFlowAnalysis orderFlow) {
        int trendScore = scoreTrend(technical);
        int momentumScore = scoreMomentum(technical.rsi14());
        int orderFlowScore = scoreOrderFlow(orderFlow);
        int volumeScore = scoreVolume(technical);
        int breakoutScore = scoreBreakout(technical.breakoutSignal());
        int trendlineScore = scoreTrendline(technical.trendlineAnalysis().uptrend(), technical.trendlineAnalysis().downtrend());
        int total = clamp(BASE_SCORE + trendScore + momentumScore + orderFlowScore + volumeScore + breakoutScore + trendlineScore, 0, 100);
        String bias = detectBias(total);
        String risk = detectRisk(total, technical, orderFlow);

        return new SignalScore(
                technical.symbol(),
                technical.pair(),
                technical.interval(),
                total,
                bias,
                risk,
                trendScore,
                momentumScore,
                orderFlowScore,
                volumeScore,
                breakoutScore,
                trendlineScore,
                buildSummary(technical, orderFlow, total, bias)
        );
    }

    private int scoreTrend(TechnicalAnalysis technical) {
        BigDecimal price = technical.lastClose();
        BigDecimal ema20 = lastValue(technical.ema20());
        BigDecimal ema50 = lastValue(technical.ema50());

        int score = 0;
        score += price.compareTo(ema20) >= 0 ? 8 : -8;
        score += price.compareTo(ema50) >= 0 ? 7 : -7;
        score += ema20.compareTo(ema50) >= 0 ? 7 : -7;

        return clamp(score, -22, 22);
    }

    private int scoreMomentum(BigDecimal rsi) {
        if (rsi.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return 6;
        }
        if (rsi.compareTo(BigDecimal.valueOf(55)) >= 0) {
            return 12;
        }
        if (rsi.compareTo(BigDecimal.valueOf(45)) >= 0) {
            return 0;
        }
        if (rsi.compareTo(BigDecimal.valueOf(30)) >= 0) {
            return -12;
        }

        return -6;
    }

    private int scoreOrderFlow(OrderFlowAnalysis orderFlow) {
        int score = 0;
        score += scoreRatio(orderFlow.bidDominance());
        score += scoreRatio(orderFlow.tradeBuyRatio());

        if (orderFlow.fundingRate().compareTo(BigDecimal.ZERO) > 0) {
            score += 2;
        } else if (orderFlow.fundingRate().compareTo(BigDecimal.ZERO) < 0) {
            score -= 2;
        }

        return clamp(score, -20, 20);
    }

    private int scoreRatio(BigDecimal ratio) {
        if (ratio.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            return 10;
        }
        if (ratio.compareTo(BigDecimal.valueOf(0.6)) >= 0) {
            return 7;
        }
        if (ratio.compareTo(BigDecimal.valueOf(0.4)) > 0) {
            return 0;
        }
        if (ratio.compareTo(BigDecimal.valueOf(0.3)) > 0) {
            return -7;
        }

        return -10;
    }

    private int scoreVolume(TechnicalAnalysis technical) {
        int score = 0;
        score += technical.lastVolumeDelta().compareTo(BigDecimal.ZERO) > 0 ? 5 : -5;
        score += technical.totalVolumeDelta().compareTo(BigDecimal.ZERO) > 0 ? 4 : -4;

        BigDecimal volumeRatio = technical.breakoutSignal().volumeRatio();
        if (volumeRatio.compareTo(BigDecimal.valueOf(1.2)) >= 0) {
            score += 5;
        } else if (volumeRatio.compareTo(BigDecimal.valueOf(0.7)) < 0) {
            score -= 4;
        }

        return clamp(score, -14, 14);
    }

    private int scoreBreakout(BreakoutSignal breakout) {
        if ("Bullish Breakout".equals(breakout.direction())) {
            return breakout.confirmed() ? 15 : 5;
        }

        if ("Bearish Breakdown".equals(breakout.direction())) {
            return breakout.confirmed() ? -15 : -5;
        }

        return 0;
    }

    private int scoreTrendline(Trendline uptrend, Trendline downtrend) {
        int score = 0;
        if (uptrend != null && uptrend.active()) {
            score += Math.min(12, 5 + uptrend.touches() * 2);
        }

        if (downtrend != null && downtrend.active()) {
            score -= Math.min(12, 5 + downtrend.touches() * 2);
        }

        return clamp(score, -15, 15);
    }

    private String detectBias(int totalScore) {
        if (totalScore >= 65) {
            return "Bullish";
        }

        if (totalScore <= 35) {
            return "Bearish";
        }

        return "Neutral";
    }

    private String detectRisk(int totalScore, TechnicalAnalysis technical, OrderFlowAnalysis orderFlow) {
        boolean rsiExtreme = technical.rsi14().compareTo(BigDecimal.valueOf(70)) >= 0
                || technical.rsi14().compareTo(BigDecimal.valueOf(30)) <= 0;
        boolean weakVolume = technical.breakoutSignal().volumeRatio().compareTo(BigDecimal.valueOf(0.7)) < 0;
        boolean conflict = isDirectionConflict(technical, orderFlow);

        if (rsiExtreme || conflict || weakVolume && (totalScore >= 65 || totalScore <= 35)) {
            return "High";
        }

        if (technical.breakoutSignal().confirmed()) {
            return "Medium";
        }

        if (totalScore > 42 && totalScore < 58) {
            return "Medium";
        }

        return "Low";
    }

    private boolean isDirectionConflict(TechnicalAnalysis technical, OrderFlowAnalysis orderFlow) {
        boolean trendBullish = technical.lastClose().compareTo(lastValue(technical.ema20())) >= 0
                && lastValue(technical.ema20()).compareTo(lastValue(technical.ema50())) >= 0;
        boolean trendBearish = technical.lastClose().compareTo(lastValue(technical.ema20())) < 0
                && lastValue(technical.ema20()).compareTo(lastValue(technical.ema50())) < 0;
        boolean flowBullish = orderFlow.bidDominance().compareTo(BigDecimal.valueOf(0.6)) >= 0
                && orderFlow.tradeBuyRatio().compareTo(BigDecimal.valueOf(0.6)) >= 0;
        boolean flowBearish = orderFlow.bidDominance().compareTo(BigDecimal.valueOf(0.4)) <= 0
                && orderFlow.tradeBuyRatio().compareTo(BigDecimal.valueOf(0.4)) <= 0;

        return trendBullish && flowBearish || trendBearish && flowBullish;
    }

    private String buildSummary(TechnicalAnalysis technical, OrderFlowAnalysis orderFlow, int total, String bias) {
        return "%s score %d/100. Price is %s EMA20 and %s EMA50; RSI is %s. Breakout state: %s. Order flow shows %s with %s. Treat as signal support, not financial advice."
                .formatted(
                        bias,
                        total,
                        technical.lastClose().compareTo(lastValue(technical.ema20())) >= 0 ? "above" : "below",
                        technical.lastClose().compareTo(lastValue(technical.ema50())) >= 0 ? "above" : "below",
                        formatPlain(technical.rsi14()),
                        technical.breakoutSignal().direction(),
                        orderFlow.bookPressure(),
                        orderFlow.tradePressure()
                );
    }

    private BigDecimal lastValue(Iterable<BigDecimal> values) {
        BigDecimal last = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            last = value;
        }

        return last;
    }

    private String formatMoney(BigDecimal value) {
        return "%,.2f".formatted(value);
    }

    private String formatPlain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SignalCommand(String symbol, String interval) {
    }
}
