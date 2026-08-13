package com.techatlas.service;

import com.techatlas.dto.StackOverflowDiscoverRequest;
import com.techatlas.dto.StackOverflowDiscoverResponse;
import com.techatlas.dto.StackOverflowSyncStatusResponse;

public interface StackOverflowService {
    StackOverflowDiscoverResponse discoverQuestions(StackOverflowDiscoverRequest request);
    StackOverflowSyncStatusResponse getSyncStatus();
}
