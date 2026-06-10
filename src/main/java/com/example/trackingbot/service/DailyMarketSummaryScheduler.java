package com.example.trackingbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyMarketSummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyMarketSummaryScheduler.class);

    private final DailyMarketSummaryService dailyMarketSummaryService;
    private final TelegramMessageService telegramMessageService;

    public DailyMarketSummaryScheduler(
            DailyMarketSummaryService dailyMarketSummaryService,
            TelegramMessageService telegramMessageService
    ) {
        this.dailyMarketSummaryService = dailyMarketSummaryService;
        this.telegramMessageService = telegramMessageService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailySummary() {
        String summaryMessage = dailyMarketSummaryService.buildDailySummaryMessage();
        for (Long chatId : dailyMarketSummaryService.getSubscriberChatIds()) {
            try {
                telegramMessageService.sendTextMessage(chatId, summaryMessage);
            } catch (Exception exception) {
                log.warn("Failed to send daily market summary to chat {}", chatId, exception);
            }
        }
    }
}
