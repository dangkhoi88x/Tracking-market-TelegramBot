package com.example.trackingbot.service.maintenance;

import com.example.trackingbot.config.CleanupProperties;
import com.example.trackingbot.config.IdeaChartProperties;
import com.example.trackingbot.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MaintenanceCleanupService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceCleanupService.class);
    private static final String TELEGRAM_CHANNEL_NEWS_CACHE = "telegramChannelNews";

    private final PriceAlertRepository priceAlertRepository;
    private final IdeaChartProperties ideaChartProperties;
    private final CleanupProperties cleanupProperties;
    private final CacheManager cacheManager;

    @Transactional
    public int cleanupInactiveAlerts() {
        Instant cutoff = Instant.now().minus(cleanupProperties.inactiveAlertRetentionOrDefault());
        int deletedCount = priceAlertRepository.deleteInactiveBefore(cutoff);

        log.info("Cleaned up inactive price alerts count={} cutoff={}", deletedCount, cutoff);
        return deletedCount;
    }

    public int cleanupIdeaChartTempFiles() {
        Path outputDir = Path.of(ideaChartProperties.outputDirOrDefault());
        if (!Files.exists(outputDir)) {
            log.info("Skipped idea chart temp cleanup because directory does not exist path={}", outputDir);
            return 0;
        }

        Instant cutoff = Instant.now().minus(cleanupProperties.chartFileRetentionOrDefault());
        AtomicInteger deletedCount = new AtomicInteger();

        try (Stream<Path> files = Files.find(
                outputDir,
                1,
                (path, attributes) -> attributes.isRegularFile()
                        && isIdeaChartTempFile(path)
                        && attributes.lastModifiedTime().toInstant().isBefore(cutoff)
        )) {
            files.forEach(path -> deleteTempFile(path, deletedCount));
        } catch (IOException exception) {
            log.warn("Failed to cleanup idea chart temp files path={}", outputDir, exception);
        }

        log.info("Cleaned up idea chart temp files count={} path={} cutoff={}", deletedCount.get(), outputDir, cutoff);
        return deletedCount.get();
    }

    public void clearTelegramChannelNewsCache() {
        Cache cache = cacheManager.getCache(TELEGRAM_CHANNEL_NEWS_CACHE);
        if (cache == null) {
            log.info("Skipped news cache cleanup because cache is not available cacheName={}", TELEGRAM_CHANNEL_NEWS_CACHE);
            return;
        }

        cache.clear();
        log.info("Cleared news cache cacheName={}", TELEGRAM_CHANNEL_NEWS_CACHE);
    }

    private boolean isIdeaChartTempFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith("idea-")
                && (fileName.endsWith(".png") || fileName.endsWith(".json"));
    }

    private void deleteTempFile(Path path, AtomicInteger deletedCount) {
        try {
            if (Files.deleteIfExists(path)) {
                deletedCount.incrementAndGet();
            }
        } catch (IOException exception) {
            log.warn("Failed to delete idea chart temp file path={}", path, exception);
        }
    }
}
