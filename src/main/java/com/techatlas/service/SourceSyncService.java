package com.techatlas.service;

import com.techatlas.dto.SourceSyncResponse;
import com.techatlas.dto.SourceSyncStatusResponse;
import com.techatlas.entity.SourceType;

import java.util.Set;
import java.util.UUID;

public interface SourceSyncService {
    SourceSyncResponse syncSource(SourceType source);
    void createOrUpdateSyncRecord(SourceType source, String externalId, String revision, String hash, UUID documentId);
    SourceSyncStatusResponse getStatusSummary(SourceType source);
    Set<SourceType> getRunningSources();
}
