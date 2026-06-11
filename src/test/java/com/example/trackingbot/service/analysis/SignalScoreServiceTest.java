package com.example.trackingbot.service.analysis;

import com.example.trackingbot.dto.response.BreakoutSignal;
import com.example.trackingbot.dto.response.OrderFlowAnalysis;
import com.example.trackingbot.dto.response.TechnicalAnalysis;
import com.example.trackingbot.dto.response.TrendlineAnalysis;
import com.example.trackingbot.service.crypto.CryptoPriceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignalScoreServiceTest {

    @Mock
    private TechnicalAnalysisService technicalAnalysisService;

    @Mock
    private OrderFlowService orderFlowService;

    @Mock
    private CryptoPriceService cryptoPriceService;

    @InjectMocks
    private SignalScoreService signalScoreService;

    @Test
    void getSignalMessage_shouldReturnBullishScoreWhenSignalsAreAligned() {
        when(cryptoPriceService.normalizeSymbol("BTC")).thenReturn("BTC");
        when(technicalAnalysisService.analyze("BTC", "4h")).thenReturn(bullishTechnicalAnalysis());
        when(orderFlowService.analyze("BTC")).thenReturn(bullishOrderFlow());

        String result = signalScoreService.getSignalMessage("BTC");

        assertThat(result).contains("BTC Signal Score:");
        assertThat(result).contains("Bias: Bullish");
        assertThat(result).contains("Trend: +22");
        assertThat(result).contains("Momentum: +12");
        assertThat(result).contains("Order flow: +20");
        assertThat(result).contains("Breakout: +15");
    }

    @Test
    void getSignalMessage_shouldRejectUnsupportedInterval() {
        when(cryptoPriceService.normalizeSymbol("BTC")).thenReturn("BTC");

        assertThatThrownBy(() -> signalScoreService.getSignalMessage("BTC invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TechnicalAnalysis bullishTechnicalAnalysis() {
        return new TechnicalAnalysis(
                "BTC",
                "BTCUSDT",
                "4h",
                List.of(),
                List.of(new BigDecimal("62000"), new BigDecimal("63000")),
                List.of(new BigDecimal("60000"), new BigDecimal("61000")),
                List.of(),
                new BigDecimal("65000"),
                new BigDecimal("59000"),
                new BigDecimal("66000"),
                new BigDecimal("59000"),
                new BigDecimal("64000"),
                new BigDecimal("1000"),
                new BigDecimal("60"),
                new BigDecimal("800"),
                new BigDecimal("200"),
                new BigDecimal("600"),
                new BigDecimal("2500"),
                "Bullish",
                new BreakoutSignal(
                        "Bullish Breakout",
                        true,
                        new BigDecimal("64000"),
                        new BigDecimal("65000"),
                        new BigDecimal("1000"),
                        new BigDecimal("1800"),
                        new BigDecimal("1.8"),
                        new BigDecimal("600"),
                        List.of("Breakout confirmed")
                ),
                new TrendlineAnalysis(List.of(), List.of(), null, null, "No trendline")
        );
    }

    private OrderFlowAnalysis bullishOrderFlow() {
        return new OrderFlowAnalysis(
                new BigDecimal("700"),
                new BigDecimal("300"),
                new BigDecimal("0.70"),
                new BigDecimal("800"),
                new BigDecimal("200"),
                new BigDecimal("0.80"),
                new BigDecimal("100000"),
                new BigDecimal("0.0001"),
                "Bid dominance",
                "Buy pressure",
                "Active",
                "Bid dominance | Buy pressure | positive funding"
        );
    }
}
