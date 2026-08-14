package com.techatlas.controller;

import com.techatlas.config.SyncSchedulerProperties;
import com.techatlas.dto.SourceSyncResponse;
import com.techatlas.dto.SourceSyncStatusResponse;
import com.techatlas.dto.SchedulerStatusResponse;
import com.techatlas.entity.SourceType;
import com.techatlas.service.SourceSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
public class SourceSyncController {

    private final SourceSyncService sourceSyncService;
    private final SyncSchedulerProperties schedulerProperties;

    public SourceSyncController(SourceSyncService sourceSyncService, SyncSchedulerProperties schedulerProperties) {
        this.sourceSyncService = sourceSyncService;
        this.schedulerProperties = schedulerProperties;
    }

    @PostMapping("/{source}")
    public ResponseEntity<SourceSyncResponse> syncSource(@PathVariable("source") SourceType source) {
        if (source == SourceType.MANUAL) {
            throw new IllegalArgumentException("MANUAL source type is not synchronizable.");
        }
        SourceSyncResponse response = sourceSyncService.syncSource(source);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<SourceSyncStatusResponse> getSyncStatus(
            @RequestParam(value = "source", required = false) SourceType source) {
        SourceSyncStatusResponse response = sourceSyncService.getStatusSummary(source);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/scheduler/status")
    public ResponseEntity<SchedulerStatusResponse> getSchedulerStatus() {
        Map<SourceType, Boolean> configuredSources = new HashMap<>();
        configuredSources.put(SourceType.WIKIPEDIA, schedulerProperties.getWikipedia().isEnabled());
        configuredSources.put(SourceType.GITHUB, schedulerProperties.getGithub().isEnabled());
        configuredSources.put(SourceType.STACKOVERFLOW, schedulerProperties.getStackoverflow().isEnabled());

        SchedulerStatusResponse response = new SchedulerStatusResponse(
                schedulerProperties.isEnabled(),
                sourceSyncService.getRunningSources(),
                configuredSources,
                schedulerProperties.getFixedDelayMs(),
                schedulerProperties.getInitialDelayMs()
        );
        return ResponseEntity.ok(response);
    }
}
