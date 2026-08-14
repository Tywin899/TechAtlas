package com.techatlas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_analytics", indexes = {
    @Index(name = "idx_search_analytics_normalized_query", columnList = "normalized_query"),
    @Index(name = "idx_search_analytics_timestamp", columnList = "timestamp"),
    @Index(name = "idx_search_analytics_zero_results", columnList = "zero_results")
})
public class SearchAnalytics {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "query", nullable = false, length = 500)
    private String query;

    @Column(name = "normalized_query", nullable = false, length = 500)
    private String normalizedQuery;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "result_count", nullable = false)
    private long resultCount;

    @Column(name = "requested_page", nullable = false)
    private int requestedPage;

    @Column(name = "requested_size", nullable = false)
    private int requestedSize;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "zero_results", nullable = false)
    private boolean zeroResults;

    @Column(name = "served_from_cache", nullable = false)
    private boolean servedFromCache;

    public SearchAnalytics() {}

    public SearchAnalytics(UUID id, String query, String normalizedQuery, LocalDateTime timestamp, 
                           long resultCount, int requestedPage, int requestedSize, long latencyMs, 
                           boolean zeroResults, boolean servedFromCache) {
        this.id = id;
        this.query = query;
        this.normalizedQuery = normalizedQuery;
        this.timestamp = timestamp;
        this.resultCount = resultCount;
        this.requestedPage = requestedPage;
        this.requestedSize = requestedSize;
        this.latencyMs = latencyMs;
        this.zeroResults = zeroResults;
        this.servedFromCache = servedFromCache;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public void setNormalizedQuery(String normalizedQuery) {
        this.normalizedQuery = normalizedQuery;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getResultCount() {
        return resultCount;
    }

    public void setResultCount(long resultCount) {
        this.resultCount = resultCount;
    }

    public int getRequestedPage() {
        return requestedPage;
    }

    public void setRequestedPage(int requestedPage) {
        this.requestedPage = requestedPage;
    }

    public int getRequestedSize() {
        return requestedSize;
    }

    public void setRequestedSize(int requestedSize) {
        this.requestedSize = requestedSize;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public boolean isZeroResults() {
        return zeroResults;
    }

    public void setZeroResults(boolean zeroResults) {
        this.zeroResults = zeroResults;
    }

    public boolean isServedFromCache() {
        return servedFromCache;
    }

    public void setServedFromCache(boolean servedFromCache) {
        this.servedFromCache = servedFromCache;
    }
}
