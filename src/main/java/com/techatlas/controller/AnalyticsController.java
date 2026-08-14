package com.techatlas.controller;

import com.techatlas.config.AnalyticsProperties;
import com.techatlas.dto.*;
import com.techatlas.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics & Monitoring API", description = "Endpoints for retrieving system metrics, search analytics, and monitoring data")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsProperties properties;

    public AnalyticsController(AnalyticsService analyticsService, AnalyticsProperties properties) {
        this.analyticsService = analyticsService;
        this.properties = properties;
    }

    @GetMapping("/search/top-queries")
    @Operation(summary = "Get most frequently searched queries", description = "Retrieves top queries sorted by frequency descending")
    public ResponseEntity<List<TopQueryResponse>> getTopQueries(
            @RequestParam(value = "limit", required = false) Integer limit) {
        int validatedLimit = validateLimit(limit);
        return ResponseEntity.ok(analyticsService.getTopQueries(validatedLimit));
    }

    @GetMapping("/search/zero-results")
    @Operation(summary = "Get searches that returned no results", description = "Retrieves zero-result queries sorted by occurrence descending")
    public ResponseEntity<List<ZeroResultResponse>> getZeroResults(
            @RequestParam(value = "limit", required = false) Integer limit) {
        int validatedLimit = validateLimit(limit);
        return ResponseEntity.ok(analyticsService.getZeroResults(validatedLimit));
    }

    @GetMapping("/search/latency")
    @Operation(summary = "Get search execution latency stats", description = "Returns total, average, min, max and percentiles (p50, p90, p95, p99) search latencies")
    public ResponseEntity<SearchLatencyResponse> getSearchLatency() {
        return ResponseEntity.ok(analyticsService.getSearchLatency());
    }

    @GetMapping("/cache")
    @Operation(summary = "Get Redis cache hits, misses and status details", description = "Calculates hit ratio and exposes Redis availability status")
    public ResponseEntity<CacheAnalyticsResponse> getCacheAnalytics() {
        return ResponseEntity.ok(analyticsService.getCacheAnalytics());
    }

    @GetMapping("/documents")
    @Operation(summary = "Get corpus document status and source statistics", description = "Retrieves total documents grouped by source type, status and category")
    public ResponseEntity<DocumentStatsResponse> getDocumentStats() {
        return ResponseEntity.ok(analyticsService.getDocumentStats());
    }

    @GetMapping("/index")
    @Operation(summary = "Get inverted index statistics and indexing operation counters", description = "Exposes attempts, successes, failures, and vocab size metrics")
    public ResponseEntity<IndexMetricsResponse> getIndexMetrics() {
        return ResponseEntity.ok(analyticsService.getIndexMetrics());
    }

    @GetMapping("/synchronization")
    @Operation(summary = "Get sync engine health status", description = "Exposes status, last checked, last synced, and operation counts for all sources")
    public ResponseEntity<SyncHealthResponse> getSyncHealth() {
        return ResponseEntity.ok(analyticsService.getSyncHealth());
    }

    @GetMapping("/overview")
    @Operation(summary = "Get a unified high-level system analytics overview", description = "Composes composed metrics from search, document, index, cache and sync sub-systems")
    public ResponseEntity<OverviewAnalyticsResponse> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }

    private int validateLimit(Integer limit) {
        int limitVal = limit != null ? limit : properties.getDefaultLimit();
        if (limitVal <= 0 || limitVal > properties.getMaxLimit()) {
            throw new IllegalArgumentException("Limit must be positive and not exceed " + properties.getMaxLimit());
        }
        return limitVal;
    }
}
