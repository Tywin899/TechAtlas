package com.techatlas.service;

import com.techatlas.dto.SourceSyncResponse;
import com.techatlas.dto.SourceSyncStatusResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.SourceSyncRecord;
import com.techatlas.entity.SourceType;
import com.techatlas.entity.SyncStatus;
import com.techatlas.index.IndexService;
import com.techatlas.repository.SourceSyncRecordRepository;
import com.techatlas.sync.SourceResource;
import com.techatlas.sync.SourceSynchronizer;
import com.techatlas.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SourceSyncServiceImpl implements SourceSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SourceSyncServiceImpl.class);

    private final SourceSyncRecordRepository repository;
    private final DocumentService documentService;
    private final IndexService indexService;
    private final Map<SourceType, SourceSynchronizer> synchronizersMap;
    private final Set<SourceType> runningSources = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<SourceType, SourceSyncResponse> lastSyncRuns = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<SourceType, Long> lastSyncDurations = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<SourceType, String> lastSyncStatuses = new java.util.concurrent.ConcurrentHashMap<>();

    public SourceSyncServiceImpl(
            SourceSyncRecordRepository repository,
            DocumentService documentService,
            IndexService indexService,
            List<SourceSynchronizer> synchronizers) {
        this.repository = repository;
        this.documentService = documentService;
        this.indexService = indexService;

        Map<SourceType, SourceSynchronizer> map = new EnumMap<>(SourceType.class);
        for (SourceSynchronizer synchronizer : synchronizers) {
            map.put(synchronizer.getSource(), synchronizer);
        }
        this.synchronizersMap = Collections.unmodifiableMap(map);
    }

    @Override
    @Transactional
    public SourceSyncResponse syncSource(SourceType source) {
        if (!runningSources.add(source)) {
            throw new IllegalStateException("Synchronization is already running for source: " + source);
        }
        long startTime = System.nanoTime();
        SourceSyncResponse response = null;
        try {
            SourceSynchronizer synchronizer = synchronizersMap.get(source);
            if (synchronizer == null) {
                throw new IllegalArgumentException("No synchronizer registered for source: " + source);
            }

            List<SourceSyncRecord> records = repository.findBySource(source);

            int checked = 0;
            int newResources = 0;
            int changedResources = 0;
            int unchangedResources = 0;
            int skippedResources = 0;
            int failedResources = 0;
            int createdDocuments = 0;
            int updatedDocuments = 0;
            int indexedDocuments = 0;

            for (SourceSyncRecord record : records) {
                checked++;
                try {
                    String originalTitle = "";
                    if (record.getDocumentId() != null) {
                        try {
                            originalTitle = documentService.retrieve(record.getDocumentId()).title();
                        } catch (Exception e) {
                            logger.warn("Document ID [{}] not found for sync record [{}]: {}", record.getDocumentId(), record.getId(), e.getMessage());
                        }
                    }

                    SourceResource resource = synchronizer.fetchResource(record.getExternalId(), originalTitle);
                    if (resource == null) {
                        record.setStatus(SyncStatus.SKIPPED);
                        record.setLastCheckedAt(LocalDateTime.now());
                        repository.save(record);
                        skippedResources++;
                        continue;
                    }

                    String currentHash = HashUtil.calculateSha256(resource.content());

                    boolean changed;
                    if (resource.externalRevision() != null && record.getExternalRevision() != null) {
                        changed = !resource.externalRevision().equals(record.getExternalRevision());
                    } else {
                        changed = !currentHash.equals(record.getContentHash());
                    }

                    if (changed) {
                        if (record.getDocumentId() != null) {
                            UpdateDocumentRequest updateRequest = new UpdateDocumentRequest(
                                    resource.title(),
                                    resource.content(),
                                    resource.url(),
                                    resource.source(),
                                    resource.category(),
                                    resource.author(),
                                    resource.language(),
                                    resource.metadata()
                            );
                            documentService.update(record.getDocumentId(), updateRequest);
                            indexService.indexDocument(record.getDocumentId());
                            updatedDocuments++;
                            indexedDocuments++;
                        }
                        changedResources++;

                        record.setExternalRevision(resource.externalRevision());
                        record.setContentHash(currentHash);
                        record.setStatus(SyncStatus.CHANGED);
                        record.setLastCheckedAt(LocalDateTime.now());
                        record.setLastSyncedAt(LocalDateTime.now());
                        record.setLastError(null);
                    } else {
                        unchangedResources++;
                        record.setStatus(SyncStatus.SYNCED);
                        record.setLastCheckedAt(LocalDateTime.now());
                        record.setLastError(null);
                    }

                    repository.save(record);

                } catch (Exception e) {
                    logger.error("Failed to synchronize record [{}] for source [{}]: {}", record.getId(), source, e.getMessage());
                    failedResources++;
                    record.setStatus(SyncStatus.FAILED);
                    record.setLastCheckedAt(LocalDateTime.now());
                    record.setLastError(e.getMessage());
                    repository.save(record);
                }
            }

            response = new SourceSyncResponse(
                    source,
                    checked,
                    newResources,
                    changedResources,
                    unchangedResources,
                    skippedResources,
                    failedResources,
                    createdDocuments,
                    updatedDocuments,
                    indexedDocuments
            );
            lastSyncRuns.put(source, response);
            lastSyncStatuses.put(source, "SUCCESS");
            return response;
        } catch (Exception e) {
            lastSyncStatuses.put(source, "FAILED");
            throw e;
        } finally {
            long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            lastSyncDurations.put(source, durationMs);
            runningSources.remove(source);
        }
    }

    @Override
    @Transactional
    public void createOrUpdateSyncRecord(SourceType source, String externalId, String revision, String hash, UUID documentId) {
        Optional<SourceSyncRecord> opt = repository.findBySourceAndExternalId(source, externalId);
        SourceSyncRecord record;
        if (opt.isPresent()) {
            record = opt.get();
            record.setExternalRevision(revision);
            record.setContentHash(hash);
            record.setDocumentId(documentId);
            record.setStatus(SyncStatus.SYNCED);
            record.setLastCheckedAt(LocalDateTime.now());
            record.setLastSyncedAt(LocalDateTime.now());
            record.setLastError(null);
        } else {
            record = new SourceSyncRecord(source, externalId, documentId, revision, hash, SyncStatus.SYNCED);
            record.setLastCheckedAt(LocalDateTime.now());
            record.setLastSyncedAt(LocalDateTime.now());
        }
        repository.save(record);
    }

    @Override
    public SourceSyncStatusResponse getStatusSummary(SourceType source) {
        List<SourceSyncRecord> list = (source == null) ? repository.findAll() : repository.findBySource(source);
        long totalTracked = list.size();

        List<SourceSyncStatusResponse.SyncStatusInfo> records = list.stream()
                .map(r -> {
                    String title = "";
                    if (r.getDocumentId() != null) {
                        try {
                            title = documentService.retrieve(r.getDocumentId()).title();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    return new SourceSyncStatusResponse.SyncStatusInfo(
                            r.getSource(),
                            r.getExternalId(),
                            title,
                            r.getLastCheckedAt(),
                            r.getLastSyncedAt(),
                            r.getStatus(),
                            r.getLastError()
                    );
                })
                .toList();

        return new SourceSyncStatusResponse(totalTracked, records);
    }

    @Override
    public Set<SourceType> getRunningSources() {
        return Collections.unmodifiableSet(runningSources);
    }

    @Override
    public Map<SourceType, SourceSyncResponse> getLastSyncRuns() {
        return Collections.unmodifiableMap(lastSyncRuns);
    }

    @Override
    public Map<SourceType, Long> getLastSyncDurations() {
        return Collections.unmodifiableMap(lastSyncDurations);
    }

    @Override
    public Map<SourceType, String> getLastSyncStatuses() {
        return Collections.unmodifiableMap(lastSyncStatuses);
    }
}
