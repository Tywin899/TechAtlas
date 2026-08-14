package com.techatlas.dto;

public record IndexMetricsResponse(
    long indexedDocuments,
    long pendingDocuments,
    long failedDocuments,
    long indexOperations,
    long failedOperations,
    double averageIndexLatencyMs,
    long vocabularySize,
    long totalPostings
) {}
