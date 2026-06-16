package com.example.trackingbot.service.telegram;

import com.example.trackingbot.dto.request.InlineKeyboardButton;
import com.example.trackingbot.dto.request.InlineKeyboardMarkup;
import com.example.trackingbot.service.news.TelegramChannelNewsService.NewsPage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramKeyboardService {

    public InlineKeyboardMarkup buildMainMenuKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Market", "MENU:MARKET"),
                        new InlineKeyboardButton("Charts", "MENU:CHARTS")
                ),
                List.of(
                        new InlineKeyboardButton("Watchlist", "MENU:WATCHLIST"),
                        new InlineKeyboardButton("Alerts", "MENU:ALERTS")
                ),
                List.of(
                        new InlineKeyboardButton("Portfolio", "MENU:PORTFOLIO"),
                        new InlineKeyboardButton("News", "MENU:NEWS")
                ),
                List.of(
                        new InlineKeyboardButton("AI", "MENU:AI"),
                        new InlineKeyboardButton("Settings", "MENU:SETTINGS")
                )
        ));
    }

    public InlineKeyboardMarkup buildMarketSymbolKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("BTC", "MARKET_SYMBOL:BTC"),
                        new InlineKeyboardButton("ETH", "MARKET_SYMBOL:ETH"),
                        new InlineKeyboardButton("SOL", "MARKET_SYMBOL:SOL")
                ),
                List.of(
                        new InlineKeyboardButton("BNB", "MARKET_SYMBOL:BNB"),
                        new InlineKeyboardButton("XRP", "MARKET_SYMBOL:XRP"),
                        new InlineKeyboardButton("DOGE", "MARKET_SYMBOL:DOGE")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    public InlineKeyboardMarkup buildChartSymbolKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("BTC 7d", "CHART:BTC:7d"),
                        new InlineKeyboardButton("ETH 7d", "CHART:ETH:7d"),
                        new InlineKeyboardButton("SOL 7d", "CHART:SOL:7d")
                ),
                List.of(
                        new InlineKeyboardButton("BNB 7d", "CHART:BNB:7d"),
                        new InlineKeyboardButton("XRP 7d", "CHART:XRP:7d"),
                        new InlineKeyboardButton("DOGE 7d", "CHART:DOGE:7d")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    public InlineKeyboardMarkup buildAiSymbolKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("BTC AI", "AI_PREDICTION:BTC:4h"),
                        new InlineKeyboardButton("ETH AI", "AI_PREDICTION:ETH:4h"),
                        new InlineKeyboardButton("SOL AI", "AI_PREDICTION:SOL:4h")
                ),
                List.of(
                        new InlineKeyboardButton("AI Chart BTC", "AI_CHART:BTC:4h"),
                        new InlineKeyboardButton("AI Chart ETH", "AI_CHART:ETH:4h")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    public InlineKeyboardMarkup buildSymbolDashboardKeyboard(String symbol) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Price", "PRICE:" + symbol),
                        new InlineKeyboardButton("Chart 7d", "CHART:" + symbol + ":7d")
                ),
                List.of(
                        new InlineKeyboardButton("AI", "AI_PREDICTION:" + symbol + ":4h"),
                        new InlineKeyboardButton("AI Chart", "AI_CHART:" + symbol + ":4h")
                ),
                List.of(
                        new InlineKeyboardButton("Signal", "SIGNAL:" + symbol),
                        new InlineKeyboardButton("Watch", "WATCH:" + symbol)
                ),
                List.of(
                        new InlineKeyboardButton("Alert", "ALERT_PROMPT:" + symbol),
                        new InlineKeyboardButton("Main Menu", "MENU:MAIN")
                )
        ));
    }

    public InlineKeyboardMarkup buildWatchlistKeyboard(List<String> symbols) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String symbol : symbols) {
            rows.add(List.of(
                    new InlineKeyboardButton(symbol + " Price", "PRICE:" + symbol),
                    new InlineKeyboardButton("Chart", "CHART:" + symbol + ":7d"),
                    new InlineKeyboardButton("AI", "AI_PREDICTION:" + symbol + ":4h")
            ));
            rows.add(List.of(
                    new InlineKeyboardButton("Signal", "SIGNAL:" + symbol),
                    new InlineKeyboardButton("Unwatch " + symbol, "UNWATCH:" + symbol)
            ));
        }
        rows.add(List.of(
                new InlineKeyboardButton("Alert Builder", "MENU:ALERTS"),
                new InlineKeyboardButton("Main Menu", "MENU:MAIN")
        ));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup buildEmptyWatchlistKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Watch BTC", "WATCH:BTC"),
                        new InlineKeyboardButton("Watch ETH", "WATCH:ETH"),
                        new InlineKeyboardButton("Watch SOL", "WATCH:SOL")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    public InlineKeyboardMarkup buildAlertMenuKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Alert Builder", "ALERT_BUILDER:START"),
                        new InlineKeyboardButton("BTC Alert", "ALERT_PROMPT:BTC")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    public InlineKeyboardMarkup buildPortfolioMenuKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("BTC Price", "PRICE:BTC"),
                        new InlineKeyboardButton("BTC Chart", "CHART:BTC:7d")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    public InlineKeyboardMarkup buildSettingsKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("My Usage", "MENU:USAGE"),
                        new InlineKeyboardButton("Notifications", "MENU:NOTIFICATIONS")
                ),
                List.of(
                        new InlineKeyboardButton("Daily On", "MENU:DAILY_ON"),
                        new InlineKeyboardButton("Daily Off", "MENU:DAILY_OFF")
                ),
                List.of(
                        new InlineKeyboardButton("Watch Updates On", "MENU:WATCH_UPDATES_ON"),
                        new InlineKeyboardButton("Watch Updates Off", "MENU:WATCH_UPDATES_OFF")
                ),
                List.of(new InlineKeyboardButton("Main Menu", "MENU:MAIN"))
        ));
    }

    public InlineKeyboardMarkup buildCryptoKeyboard(String symbol) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Chart 7d", "CHART:" + symbol + ":7d"),
                        new InlineKeyboardButton("Chart 1m", "CHART:" + symbol + ":1m")
                ),
                List.of(
                        new InlineKeyboardButton("Add Watchlist", "WATCH:" + symbol),
                        new InlineKeyboardButton("Create Alert", "ALERT_PROMPT:" + symbol)
                ),
                List.of(
                        new InlineKeyboardButton("AI", "AI_PREDICTION:" + symbol + ":4h"),
                        new InlineKeyboardButton("Signal", "SIGNAL:" + symbol)
                )
        ));
    }

    public InlineKeyboardMarkup buildIdeaChartKeyboard(String symbol, String interval, String currentType) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton(
                                currentType.equals("IDEA") ? "Idea Chart" : "Idea",
                                "IDEA_CHART:IDEA:" + symbol + ":" + interval
                        ),
                        new InlineKeyboardButton(
                                currentType.equals("VOLUME") ? "Volume Delta" : "Volume",
                                "IDEA_CHART:VOLUME:" + symbol + ":" + interval
                        )
                ),
                List.of(
                        new InlineKeyboardButton(
                                currentType.equals("BREAKOUT") ? "Breakout Chart" : "Breakout",
                                "IDEA_CHART:BREAKOUT:" + symbol + ":" + interval
                        ),
                        new InlineKeyboardButton(
                                currentType.equals("TRENDLINE") ? "Trendline Chart" : "Trendline",
                                "IDEA_CHART:TRENDLINE:" + symbol + ":" + interval
                        )
                ),
                List.of(
                        new InlineKeyboardButton(
                                currentType.equals("ORDER_FLOW") ? "Order Flow Chart" : "Order Flow",
                                "IDEA_CHART:ORDER_FLOW:" + symbol + ":" + interval
                        )
                ),
                List.of(
                        new InlineKeyboardButton(
                                "AI",
                                "AI_PREDICTION:" + symbol + ":" + interval
                        ),
                        new InlineKeyboardButton(
                                currentType.equals("AI_CHART") ? "AI Chart" : "AI Chart",
                                "AI_CHART:" + symbol + ":" + interval
                        )
                )
        ));
    }

    public InlineKeyboardMarkup buildNewsKeyboard(NewsPage newsPage) {
        if (newsPage.page() <= 1) {
            return new InlineKeyboardMarkup(List.of(
                    List.of(new InlineKeyboardButton("Next", "NEWS_PAGE:" + (newsPage.page() + 1)))
            ));
        }

        if (newsPage.page() >= newsPage.totalPages()) {
            return new InlineKeyboardMarkup(List.of(
                    List.of(new InlineKeyboardButton("Before", "NEWS_PAGE:" + (newsPage.page() - 1)))
            ));
        }

        return new InlineKeyboardMarkup(List.of(
                List.of(
                        new InlineKeyboardButton("Before", "NEWS_PAGE:" + (newsPage.page() - 1)),
                        new InlineKeyboardButton("Next", "NEWS_PAGE:" + (newsPage.page() + 1))
                )
        ));
    }
}
