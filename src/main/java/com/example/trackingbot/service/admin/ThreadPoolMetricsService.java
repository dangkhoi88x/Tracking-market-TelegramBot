package com.example.trackingbot.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@RequiredArgsConstructor
public class ThreadPoolMetricsService {

    private static final int BUSY_QUEUE_PERCENT = 70;
    private static final int OVERLOADED_QUEUE_PERCENT = 90;

    private final ThreadPoolTaskScheduler trackingTaskScheduler;
    private final ThreadPoolTaskExecutor telegramTaskExecutor;

    public String buildMetricsMessage() {
        ScheduledThreadPoolExecutor schedulerExecutor = trackingTaskScheduler.getScheduledThreadPoolExecutor();
        ThreadPoolExecutor telegramExecutor = telegramTaskExecutor.getThreadPoolExecutor();

        return """
                Status: %s
                Scheduler: pool %d | active %d | queued %d | completed %d
                Telegram Async: pool %d/%d | active %d | queued %d/%d | completed %d
                """.formatted(
                detectStatus(schedulerExecutor, telegramExecutor),
                schedulerExecutor.getPoolSize(),
                schedulerExecutor.getActiveCount(),
                schedulerExecutor.getQueue().size(),
                schedulerExecutor.getCompletedTaskCount(),
                telegramExecutor.getPoolSize(),
                telegramExecutor.getMaximumPoolSize(),
                telegramExecutor.getActiveCount(),
                telegramExecutor.getQueue().size(),
                telegramExecutor.getQueue().size() + telegramExecutor.getQueue().remainingCapacity(),
                telegramExecutor.getCompletedTaskCount()
        ).stripTrailing();
    }

    private String detectStatus(ScheduledThreadPoolExecutor schedulerExecutor, ThreadPoolExecutor telegramExecutor) {
        int telegramQueueUsagePercent = calculateQueueUsagePercent(telegramExecutor);
        boolean schedulerBusy = schedulerExecutor.getPoolSize() > 0
                && schedulerExecutor.getActiveCount() >= schedulerExecutor.getPoolSize();

        if (telegramQueueUsagePercent >= OVERLOADED_QUEUE_PERCENT) {
            return "OVERLOADED";
        }

        if (telegramQueueUsagePercent >= BUSY_QUEUE_PERCENT || schedulerBusy) {
            return "BUSY";
        }

        return "OK";
    }

    private int calculateQueueUsagePercent(ThreadPoolExecutor executor) {
        int queueSize = executor.getQueue().size();
        int queueCapacity = queueSize + executor.getQueue().remainingCapacity();
        if (queueCapacity <= 0) {
            return 0;
        }

        return queueSize * 100 / queueCapacity;
    }
}
