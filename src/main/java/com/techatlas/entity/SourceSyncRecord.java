package com.techatlas.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "source_sync", uniqueConstraints = {
    @UniqueConstraint(name = "uq_source_sync_source_ext_id", columnNames = {"source", "external_id"})
})
public class SourceSyncRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private SourceType source;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "external_revision")
    private String externalRevision;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "last_checked_at", nullable = false)
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SyncStatus status;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SourceSyncRecord() {}

    public SourceSyncRecord(SourceType source, String externalId, UUID documentId, String externalRevision, String contentHash, SyncStatus status) {
        this.source = source;
        this.externalId = externalId;
        this.documentId = documentId;
        this.externalRevision = externalRevision;
        this.contentHash = contentHash;
        this.status = status;
        this.lastCheckedAt = LocalDateTime.now();
        this.lastSyncedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SourceType getSource() {
        return source;
    }

    public void setSource(SourceType source) {
        this.source = source;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getExternalRevision() {
        return externalRevision;
    }

    public void setExternalRevision(String externalRevision) {
        this.externalRevision = externalRevision;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
