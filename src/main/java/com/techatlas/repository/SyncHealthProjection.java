package com.techatlas.repository;

import java.time.LocalDateTime;

public interface SyncHealthProjection {
    LocalDateTime getLastCheckedAt();
    LocalDateTime getLastSyncedAt();
    Long getTotalChecked();
    Long getFailures();
    Long getChanged();
    Long getSynced();
    Long getSkipped();
}
