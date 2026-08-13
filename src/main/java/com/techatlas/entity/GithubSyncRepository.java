package com.techatlas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "github_sync_repositories")
public class GithubSyncRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "github_repo_id", nullable = false, unique = true)
    private Long githubRepoId;

    @Column(name = "repository_name", nullable = false)
    private String repositoryName;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Column(name = "document_id")
    private UUID documentId;

    public GithubSyncRepository() {}

    public GithubSyncRepository(Long githubRepoId, String repositoryName, LocalDateTime lastSyncedAt, UUID documentId) {
        this.githubRepoId = githubRepoId;
        this.repositoryName = repositoryName;
        this.lastSyncedAt = lastSyncedAt;
        this.documentId = documentId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getGithubRepoId() {
        return githubRepoId;
    }

    public void setGithubRepoId(Long githubRepoId) {
        this.githubRepoId = githubRepoId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }
}
