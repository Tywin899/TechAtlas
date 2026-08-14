package com.techatlas.service;

import com.techatlas.dto.*;
import java.util.List;

public interface AnalyticsService {
    void recordSearch(String query, long resultCount, int page, int size, long latencyMs, boolean servedFromCache);
    
    List<TopQueryResponse> getTopQueries(int limit);
    List<ZeroResultResponse> getZeroResults(int limit);
    SearchLatencyResponse getSearchLatency();
    CacheAnalyticsResponse getCacheAnalytics();
    DocumentStatsResponse getDocumentStats();
    IndexMetricsResponse getIndexMetrics();
    SyncHealthResponse getSyncHealth();
    SchedulerStatusResponse getSchedulerStatus();
    OverviewAnalyticsResponse getOverview();
}
