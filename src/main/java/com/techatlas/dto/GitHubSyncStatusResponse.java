package com.techatlas.dto;

import java.util.List;

public record GitHubSyncStatusResponse(
    long totalSyncedRepositories,
    List<RepositorySyncInfo> repositories
) {
    public record RepositorySyncInfo(
        Long githubRepoId,
        String repositoryName,
        java.time.LocalDateTime lastSyncedAt
    ) {}
}
