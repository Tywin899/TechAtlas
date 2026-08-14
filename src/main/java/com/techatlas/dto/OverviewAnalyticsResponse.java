package com.techatlas.dto;

public record OverviewAnalyticsResponse(
    SearchOverview search,
    DocumentOverview documents,
    IndexOverview index,
    CacheOverview cache,
    SyncOverview synchronization
) {
    public record SearchOverview(long totalQueries, long zeroResultQueries, double averageLatencyMs) {}
    public record DocumentOverview(long total, long active, long pending, long failed) {}
    public record IndexOverview(long vocabularySize, long totalPostings) {}
    public record CacheOverview(long hits, long misses, double hitRatio) {}
    public record SyncOverview(long healthySources, long failedSources) {}
}
