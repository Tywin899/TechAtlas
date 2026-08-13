package com.techatlas.service;

import com.techatlas.dto.GitHubDiscoverRequest;
import com.techatlas.dto.GitHubDiscoverResponse;
import com.techatlas.dto.GitHubSyncStatusResponse;

public interface GitHubService {
    GitHubDiscoverResponse discoverRepositories(GitHubDiscoverRequest request);
    GitHubSyncStatusResponse getSyncStatus();
}
