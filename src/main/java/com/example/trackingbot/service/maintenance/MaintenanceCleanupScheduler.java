package com.example.trackingbot.service.maintenance;

import com.example.trackingbot.config.CleanupProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaintenanceCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceCleanupScheduler.class);

    private final CleanupProperties cleanupProperties;
    private final MaintenanceCleanupService maintenanceCleanupService;

    @Scheduled(cron = "${cleanup.daily-cron:0 20 3 * * *}", zone = "${cleanup.zone:Asia/Ho_Chi_Minh}")
    public void runDailyCleanup() {
        if (!cleanupProperties.enabledOrDefault()) {
            log.info("Skipped daily maintenance cleanup because cleanup.enabled=false");
            return;
        }

        log.info("Starting daily maintenance cleanup");
        maintenanceCleanupService.cleanupInactiveAlerts();
        maintenanceCleanupService.cleanupIdeaChartTempFiles();
        log.info("Finished daily maintenance cleanup");
    }

    @Scheduled(cron = "${cleanup.news-cache-cron:0 0 * * * *}", zone = "${cleanup.zone:Asia/Ho_Chi_Minh}")
    public void clearNewsCache() {
        if (!cleanupProperties.enabledOrDefault()) {
            return;
        }

        maintenanceCleanupService.clearTelegramChannelNewsCache();
    }
}
