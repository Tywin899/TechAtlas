package com.techatlas.service;

import com.techatlas.config.SyncSchedulerProperties;
import com.techatlas.dto.*;
import com.techatlas.entity.*;
import com.techatlas.index.IndexService;
import com.techatlas.model.InvertedIndex;
import com.techatlas.normalizer.TextNormalizer;
import com.techatlas.repository.*;
import com.techatlas.cache.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final SearchAnalyticsRepository searchAnalyticsRepository;
    private final DocumentRepository documentRepository;
    private final SourceSyncRecordRepository sourceSyncRecordRepository;
    private final IndexService indexService;
    private final SourceSyncService sourceSyncService;
    private final CacheService cacheService;
    private final SyncSchedulerProperties schedulerProperties;
    private final TextNormalizer textNormalizer;
    private final InvertedIndex invertedIndex;

    public AnalyticsServiceImpl(
            SearchAnalyticsRepository searchAnalyticsRepository,
            DocumentRepository documentRepository,
            SourceSyncRecordRepository sourceSyncRecordRepository,
            IndexService indexService,
            SourceSyncService sourceSyncService,
            CacheService cacheService,
            SyncSchedulerProperties schedulerProperties,
            TextNormalizer textNormalizer,
            InvertedIndex invertedIndex) {
        this.searchAnalyticsRepository = searchAnalyticsRepository;
        this.documentRepository = documentRepository;
        this.sourceSyncRecordRepository = sourceSyncRecordRepository;
        this.indexService = indexService;
        this.sourceSyncService = sourceSyncService;
        this.cacheService = cacheService;
        this.schedulerProperties = schedulerProperties;
        this.textNormalizer = textNormalizer;
        this.invertedIndex = invertedIndex;
    }

    @Override
    public void recordSearch(String query, long resultCount, int page, int size, long latencyMs, boolean servedFromCache) {
        try {
            String normalized = textNormalizer.normalize(query);
            SearchAnalytics record = new SearchAnalytics(
                    UUID.randomUUID(),
                    query,
                    normalized,
                    LocalDateTime.now(),
                    resultCount,
                    page,
                    size,
                    latencyMs,
                    resultCount == 0,
                    servedFromCache
            );
            searchAnalyticsRepository.save(record);
        } catch (Exception e) {
            // Failure Isolation Boundary: primary query flow continues on analytics recording error
            logger.error("Failed to persist search analytics: {}", e.getMessage());
        }
    }

    @Override
    public List<TopQueryResponse> getTopQueries(int limit) {
        return searchAnalyticsRepository.findTopQueries(PageRequest.of(0, limit)).stream()
                .map(p -> new TopQueryResponse(p.getQuery(), p.getCount()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ZeroResultResponse> getZeroResults(int limit) {
        return searchAnalyticsRepository.findZeroResultQueries(PageRequest.of(0, limit)).stream()
                .map(p -> new ZeroResultResponse(p.getQuery(), p.getCount(), p.getLastOccurrence()))
                .collect(Collectors.toList());
    }

    @Override
    public SearchLatencyResponse getSearchLatency() {
        LatencyStatsProjection stats = searchAnalyticsRepository.findLatencyStats();
        long total = (stats != null && stats.getTotalQueries() != null) ? stats.getTotalQueries() : 0L;
        double avg = (stats != null && stats.getAverageMs() != null) ? stats.getAverageMs() : 0.0;
        long min = (stats != null && stats.getMinMs() != null) ? stats.getMinMs() : 0L;
        long max = (stats != null && stats.getMaxMs() != null) ? stats.getMaxMs() : 0L;

        double p50 = 0.0, p90 = 0.0, p95 = 0.0, p99 = 0.0;
        if (total > 0) {
            List<Long> latencies = new ArrayList<>(searchAnalyticsRepository.findRecentLatencies(PageRequest.of(0, 10000)));
            if (!latencies.isEmpty()) {
                Collections.sort(latencies);
                p50 = getPercentile(latencies, 0.50);
                p90 = getPercentile(latencies, 0.90);
                p95 = getPercentile(latencies, 0.95);
                p99 = getPercentile(latencies, 0.99);
            }
        }

        return new SearchLatencyResponse(total, avg, min, max, p50, p90, p95, p99);
    }

    private double getPercentile(List<Long> latencies, double percentile) {
        int index = (int) Math.ceil(percentile * latencies.size()) - 1;
        index = Math.max(0, Math.min(latencies.size() - 1, index));
        return latencies.get(index);
    }

    @Override
    public CacheAnalyticsResponse getCacheAnalytics() {
        long hits = cacheService.getSearchHits() + cacheService.getDocumentHits();
        long misses = cacheService.getSearchMisses() + cacheService.getDocumentMisses();
        long evictions = cacheService.getEvictions();
        double ratio = (hits + misses == 0) ? 0.0 : (double) hits / (hits + misses);
        boolean available = false;
        try {
            available = cacheService.isAvailable();
        } catch (Exception e) {
            logger.warn("Failed to check Redis cache availability: {}", e.getMessage());
        }
        return new CacheAnalyticsResponse(hits, misses, evictions, ratio, available);
    }

    @Override
    public DocumentStatsResponse getDocumentStats() {
        long total = documentRepository.count();

        Map<SourceType, Long> bySource = new EnumMap<>(SourceType.class);
        for (SourceType source : SourceType.values()) {
            bySource.put(source, 0L);
        }
        documentRepository.countBySource().forEach(p -> bySource.put(p.getSource(), p.getCount()));

        Map<DocumentStatus, Long> byStatus = new EnumMap<>(DocumentStatus.class);
        for (DocumentStatus status : DocumentStatus.values()) {
            byStatus.put(status, 0L);
        }
        documentRepository.countByStatus().forEach(p -> byStatus.put(p.getStatus(), p.getCount()));

        long indexed = byStatus.getOrDefault(DocumentStatus.ACTIVE, 0L);
        long pending = byStatus.getOrDefault(DocumentStatus.PENDING_INDEX, 0L);
        long failed = byStatus.getOrDefault(DocumentStatus.FAILED, 0L);

        Map<String, Long> byCategory = new HashMap<>();
        documentRepository.countByCategory().forEach(p -> byCategory.put(p.getCategory(), p.getCount()));

        return new DocumentStatsResponse(total, bySource, byStatus, indexed, pending, failed, byCategory);
    }

    @Override
    public IndexMetricsResponse getIndexMetrics() {
        DocumentStatsResponse docStats = getDocumentStats();

        long attempts = indexService.getIndexingAttempts();
        long success = indexService.getSuccessfulIndexOperations();
        long failedOps = indexService.getFailedIndexOperations();
        double latency = indexService.getAverageIndexLatencyMs();

        long vocabSize = invertedIndex.getIndex().size();
        long totalPostings = 0L;
        for (var list : invertedIndex.getIndex().values()) {
            totalPostings += list.getPostings().size();
        }

        return new IndexMetricsResponse(
                docStats.indexedDocuments(),
                docStats.pendingDocuments(),
                docStats.failedDocuments(),
                attempts,
                failedOps,
                latency,
                vocabSize,
                totalPostings
        );
    }

    @Override
    public SyncHealthResponse getSyncHealth() {
        List<SourceHealthItem> items = new ArrayList<>();

        for (SourceType source : SourceType.values()) {
            if (source == SourceType.MANUAL) {
                continue;
            }

            SyncHealthProjection projection = sourceSyncRecordRepository.getHealthStatsBySource(source);
            boolean isRunning = sourceSyncService.getRunningSources().contains(source);
            String status = "NEW";
            if (isRunning) {
                status = "RUNNING";
            } else {
                String lastStatus = sourceSyncService.getLastSyncStatuses().get(source);
                if (lastStatus != null) {
                    status = lastStatus;
                } else if (projection != null && projection.getTotalChecked() != null && projection.getTotalChecked() > 0) {
                    status = (projection.getFailures() != null && projection.getFailures() > 0) ? "FAILED" : "SUCCESS";
                }
            }

            LocalDateTime lastChecked = projection != null ? projection.getLastCheckedAt() : null;
            LocalDateTime lastSynced = projection != null ? projection.getLastSyncedAt() : null;
            long checked = (projection != null && projection.getTotalChecked() != null) ? projection.getTotalChecked() : 0L;
            long changed = (projection != null && projection.getChanged() != null) ? projection.getChanged() : 0L;
            long unchanged = (projection != null && projection.getSynced() != null) ? projection.getSynced() : 0L;
            long failures = (projection != null && projection.getFailures() != null) ? projection.getFailures() : 0L;
            long duration = sourceSyncService.getLastSyncDurations().getOrDefault(source, 0L);

            items.add(new SourceHealthItem(
                    source,
                    lastChecked,
                    lastSynced,
                    status,
                    checked,
                    changed,
                    unchanged,
                    failures,
                    duration
            ));
        }

        return new SyncHealthResponse(items);
    }

    @Override
    public SchedulerStatusResponse getSchedulerStatus() {
        Map<SourceType, Boolean> configured = new HashMap<>();
        configured.put(SourceType.WIKIPEDIA, schedulerProperties.getWikipedia().isEnabled());
        configured.put(SourceType.GITHUB, schedulerProperties.getGithub().isEnabled());
        configured.put(SourceType.STACKOVERFLOW, schedulerProperties.getStackoverflow().isEnabled());

        return new SchedulerStatusResponse(
                schedulerProperties.isEnabled(),
                sourceSyncService.getRunningSources(),
                configured,
                schedulerProperties.getFixedDelayMs(),
                schedulerProperties.getInitialDelayMs()
        );
    }

    @Override
    public OverviewAnalyticsResponse getOverview() {
        SearchLatencyResponse latency = getSearchLatency();
        long zeroResultCount = searchAnalyticsRepository.countByZeroResults(true);

        DocumentStatsResponse docStats = getDocumentStats();
        IndexMetricsResponse indexStats = getIndexMetrics();
        CacheAnalyticsResponse cacheStats = getCacheAnalytics();
        SyncHealthResponse syncStats = getSyncHealth();

        long healthySources = 0L;
        long failedSources = 0L;
        for (SourceHealthItem source : syncStats.sources()) {
            if ("FAILED".equalsIgnoreCase(source.status())) {
                failedSources++;
            } else {
                healthySources++;
            }
        }

        return new OverviewAnalyticsResponse(
                new OverviewAnalyticsResponse.SearchOverview(latency.totalQueries(), zeroResultCount, latency.averageMs()),
                new OverviewAnalyticsResponse.DocumentOverview(docStats.totalDocuments(), docStats.indexedDocuments(), docStats.pendingDocuments(), docStats.failedDocuments()),
                new OverviewAnalyticsResponse.IndexOverview(indexStats.vocabularySize(), indexStats.totalPostings()),
                new OverviewAnalyticsResponse.CacheOverview(cacheStats.hits(), cacheStats.misses(), cacheStats.hitRatio()),
                new OverviewAnalyticsResponse.SyncOverview(healthySources, failedSources)
        );
    }
}
