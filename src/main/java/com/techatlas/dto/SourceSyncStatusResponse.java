package com.techatlas.dto;

import com.techatlas.entity.SourceType;
import com.techatlas.entity.SyncStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SourceSyncStatusResponse(
    long totalTracked,
    List<SyncStatusInfo> records
) {
    public record SyncStatusInfo(
        SourceType source,
        String externalId,
        String title,
        LocalDateTime lastCheckedAt,
        LocalDateTime lastSyncedAt,
        SyncStatus status,
        String lastError
    ) {}
}
