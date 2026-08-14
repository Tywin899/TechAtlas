package com.techatlas.repository;

import com.techatlas.entity.SourceSyncRecord;
import com.techatlas.entity.SourceType;
import com.techatlas.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceSyncRecordRepository extends JpaRepository<SourceSyncRecord, UUID> {
    Optional<SourceSyncRecord> findBySourceAndExternalId(SourceType source, String externalId);
    List<SourceSyncRecord> findBySource(SourceType source);
    List<SourceSyncRecord> findByStatus(SyncStatus status);
    List<SourceSyncRecord> findByDocumentId(UUID documentId);
    boolean existsBySourceAndExternalId(SourceType source, String externalId);
    long countBySource(SourceType source);

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "MAX(s.lastCheckedAt) as lastCheckedAt, " +
           "MAX(s.lastSyncedAt) as lastSyncedAt, " +
           "COUNT(s) as totalChecked, " +
           "SUM(CASE WHEN s.status = 'FAILED' THEN 1L ELSE 0L END) as failures, " +
           "SUM(CASE WHEN s.status = 'CHANGED' THEN 1L ELSE 0L END) as changed, " +
           "SUM(CASE WHEN s.status = 'SYNCED' THEN 1L ELSE 0L END) as synced, " +
           "SUM(CASE WHEN s.status = 'SKIPPED' THEN 1L ELSE 0L END) as skipped " +
           "FROM SourceSyncRecord s " +
           "WHERE s.source = :source")
    SyncHealthProjection getHealthStatsBySource(SourceType source);
}
