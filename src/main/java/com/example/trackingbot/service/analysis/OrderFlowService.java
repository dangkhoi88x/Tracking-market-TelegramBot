package com.example.trackingbot.service.analysis;

import com.example.trackingbot.client.BinanceFuturesClient;
import com.example.trackingbot.dto.response.AggTrade;
import com.example.trackingbot.dto.response.OrderBookLevel;
import com.example.trackingbot.dto.response.OrderFlowAnalysis;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class OrderFlowService {

    private static final int ORDER_BOOK_LIMIT = 100;
    private static final int AGG_TRADE_LIMIT = 200;

    private final BinanceFuturesClient binanceFuturesClient;

    public OrderFlowService(BinanceFuturesClient binanceFuturesClient) {
        this.binanceFuturesClient = binanceFuturesClient;
    }

    public OrderFlowAnalysis analyze(String symbol) {
        String pair = symbol + "USDT";
        List<OrderBookLevel> bids = binanceFuturesClient.getBids(pair, ORDER_BOOK_LIMIT);
        List<OrderBookLevel> asks = binanceFuturesClient.getAsks(pair, ORDER_BOOK_LIMIT);
        List<AggTrade> trades = binanceFuturesClient.getAggTrades(pair, AGG_TRADE_LIMIT);
        BigDecimal openInterest = binanceFuturesClient.getOpenInterest(pair);
        BigDecimal fundingRate = binanceFuturesClient.getFundingRate(pair);

        BigDecimal bidVolume = sumQuantity(bids);
        BigDecimal askVolume = sumQuantity(asks);
        BigDecimal bidDominance = ratio(bidVolume, bidVolume.add(askVolume));
        BigDecimal buyTradeVolume = trades.stream()
                .filter(trade -> !trade.buyerMaker())
                .map(AggTrade::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sellTradeVolume = trades.stream()
                .filter(AggTrade::buyerMaker)
                .map(AggTrade::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tradeBuyRatio = ratio(buyTradeVolume, buyTradeVolume.add(sellTradeVolume));

        String bookPressure = detectBookPressure(bidDominance);
        String tradePressure = detectTradePressure(tradeBuyRatio);
        String openInterestState = detectOpenInterestState(openInterest);

        return new OrderFlowAnalysis(
                bidVolume,
                askVolume,
                bidDominance,
                buyTradeVolume,
                sellTradeVolume,
                tradeBuyRatio,
                openInterest,
                fundingRate,
                bookPressure,
                tradePressure,
                openInterestState,
                buildSummary(bookPressure, tradePressure, fundingRate)
        );
    }

    private BigDecimal sumQuantity(List<OrderBookLevel> levels) {
        return levels.stream()
                .map(OrderBookLevel::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal ratio(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return value.divide(total, 6, RoundingMode.HALF_UP);
    }

    private String detectBookPressure(BigDecimal bidDominance) {
        if (bidDominance.compareTo(BigDecimal.valueOf(0.6)) > 0) {
            return "Bid dominance";
        }

        if (bidDominance.compareTo(BigDecimal.valueOf(0.4)) < 0) {
            return "Ask dominance";
        }

        return "Balanced book";
    }

    private String detectTradePressure(BigDecimal tradeBuyRatio) {
        if (tradeBuyRatio.compareTo(BigDecimal.valueOf(0.6)) > 0) {
            return "Buy pressure";
        }

        if (tradeBuyRatio.compareTo(BigDecimal.valueOf(0.4)) < 0) {
            return "Sell pressure";
        }

        return "Balanced trades";
    }

    private String detectOpenInterestState(BigDecimal openInterest) {
        if (openInterest.compareTo(BigDecimal.ZERO) <= 0) {
            return "Unavailable";
        }

        return "Active";
    }

    private String buildSummary(String bookPressure, String tradePressure, BigDecimal fundingRate) {
        String fundingSide = fundingRate.compareTo(BigDecimal.ZERO) >= 0 ? "positive funding" : "negative funding";
        return bookPressure + " | " + tradePressure + " | " + fundingSide;
    }
}
