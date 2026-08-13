package com.techatlas.scheduler;

import com.techatlas.config.SyncSchedulerProperties;
import com.techatlas.entity.SourceType;
import com.techatlas.service.SourceSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SyncScheduler.class);

    private final SourceSyncService sourceSyncService;
    private final SyncSchedulerProperties properties;

    public SyncScheduler(SourceSyncService sourceSyncService, SyncSchedulerProperties properties) {
        this.sourceSyncService = sourceSyncService;
        this.properties = properties;
    }

    @Scheduled(
            initialDelayString = "${sync.scheduler.initial-delay-ms:30000}",
            fixedDelayString = "${sync.scheduler.fixed-delay-ms:3600000}"
    )
    public void runScheduledSync() {
        if (!properties.isEnabled()) {
            return;
        }

        logger.info("Starting scheduled synchronization cycle");

        if (properties.getWikipedia().isEnabled()) {
            syncSourceSafely(SourceType.WIKIPEDIA);
        }

        if (properties.getGithub().isEnabled()) {
            syncSourceSafely(SourceType.GITHUB);
        }

        if (properties.getStackoverflow().isEnabled()) {
            syncSourceSafely(SourceType.STACKOVERFLOW);
        }

        logger.info("Completed scheduled synchronization cycle");
    }

    private void syncSourceSafely(SourceType source) {
        try {
            logger.info("Starting synchronization for source={}", source);
            sourceSyncService.syncSource(source);
            logger.info("Completed synchronization for source={}", source);
        } catch (IllegalStateException e) {
            logger.info("Synchronization skipped for source={} because it is already running", source);
        } catch (Exception e) {
            logger.error("Synchronization failed for source={}: {}", source, e.getMessage());
        }
    }
}
