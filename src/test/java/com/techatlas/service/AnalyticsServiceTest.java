package com.techatlas.service;

import com.techatlas.config.SyncSchedulerProperties;
import com.techatlas.dto.*;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.index.IndexService;
import com.techatlas.model.InvertedIndex;
import com.techatlas.model.PostingList;
import com.techatlas.normalizer.TextNormalizer;
import com.techatlas.repository.*;
import com.techatlas.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private SearchAnalyticsRepository searchAnalyticsRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SourceSyncRecordRepository sourceSyncRecordRepository;

    @Mock
    private IndexService indexService;

    @Mock
    private SourceSyncService sourceSyncService;

    @Mock
    private CacheService cacheService;

    @Mock
    private TextNormalizer textNormalizer;

    @Mock
    private InvertedIndex invertedIndex;

    private SyncSchedulerProperties schedulerProperties;
    private AnalyticsService analyticsService;

    @BeforeEach
    public void setUp() {
        schedulerProperties = new SyncSchedulerProperties();
        analyticsService = new AnalyticsServiceImpl(
                searchAnalyticsRepository,
                documentRepository,
                sourceSyncRecordRepository,
                indexService,
                sourceSyncService,
                cacheService,
                schedulerProperties,
                textNormalizer,
                invertedIndex
        );
    }

    @Test
    public void testRecordSearchSuccessful() {
        when(textNormalizer.normalize("Java SEARCH")).thenReturn("java search");
        analyticsService.recordSearch("Java SEARCH", 5L, 0, 10, 15L, false);
        verify(searchAnalyticsRepository, times(1)).save(any());
    }

    @Test
    public void testRecordSearchFailsGracefully() {
        when(textNormalizer.normalize("Java SEARCH")).thenReturn("java search");
        doThrow(new RuntimeException("DB offline")).when(searchAnalyticsRepository).save(any());
        
        // Assert it does not propagate the exception
        analyticsService.recordSearch("Java SEARCH", 5L, 0, 10, 15L, false);
        verify(searchAnalyticsRepository, times(1)).save(any());
    }

    @Test
    public void testGetTopQueries() {
        QueryCountProjection p1 = mock(QueryCountProjection.class);
        when(p1.getQuery()).thenReturn("java");
        when(p1.getCount()).thenReturn(25L);

        when(searchAnalyticsRepository.findTopQueries(any())).thenReturn(List.of(p1));

        List<TopQueryResponse> result = analyticsService.getTopQueries(5);
        assertThat(result).hasSize(1);
        assertEquals("java", result.get(0).query());
        assertEquals(25L, result.get(0).count());
    }

    @Test
    public void testGetSearchLatencyCalculations() {
        LatencyStatsProjection projection = mock(LatencyStatsProjection.class);
        when(projection.getTotalQueries()).thenReturn(5L);
        when(projection.getAverageMs()).thenReturn(20.4);
        when(projection.getMinMs()).thenReturn(5L);
        when(projection.getMaxMs()).thenReturn(100L);

        when(searchAnalyticsRepository.findLatencyStats()).thenReturn(projection);
        when(searchAnalyticsRepository.findRecentLatencies(any())).thenReturn(List.of(100L, 5L, 20L, 10L, 50L));

        SearchLatencyResponse response = analyticsService.getSearchLatency();
        assertEquals(5L, response.totalQueries());
        assertEquals(20.4, response.averageMs());
        assertEquals(5L, response.minMs());
        assertEquals(100L, response.maxMs());

        // Sorted: 5, 10, 20, 50, 100
        // p50: index = ceil(0.50 * 5) - 1 = 3 - 1 = 2 (Value: 20)
        assertEquals(20.0, response.p50Ms());
        // p90: index = ceil(0.90 * 5) - 1 = 5 - 1 = 4 (Value: 100)
        assertEquals(100.0, response.p90Ms());
    }

    @Test
    public void testGetCacheAnalyticsRatio() {
        when(cacheService.getSearchHits()).thenReturn(80L);
        when(cacheService.getDocumentHits()).thenReturn(20L);
        when(cacheService.getSearchMisses()).thenReturn(40L);
        when(cacheService.getDocumentMisses()).thenReturn(10L);
        when(cacheService.getEvictions()).thenReturn(5L);
        when(cacheService.isAvailable()).thenReturn(true);

        CacheAnalyticsResponse response = analyticsService.getCacheAnalytics();
        assertEquals(100L, response.hits());
        assertEquals(50L, response.misses());
        assertEquals(5L, response.evictions());
        assertEquals(100.0 / 150.0, response.hitRatio());
        assertThat(response.available()).isTrue();
    }

    @Test
    public void testGetCacheAnalyticsZeroRatio() {
        when(cacheService.getSearchHits()).thenReturn(0L);
        when(cacheService.getDocumentHits()).thenReturn(0L);
        when(cacheService.getSearchMisses()).thenReturn(0L);
        when(cacheService.getDocumentMisses()).thenReturn(0L);

        CacheAnalyticsResponse response = analyticsService.getCacheAnalytics();
        assertEquals(0.0, response.hitRatio());
    }

    @Test
    public void testGetIndexMetrics() {
        when(documentRepository.count()).thenReturn(100L);
        when(indexService.getIndexingAttempts()).thenReturn(120L);
        when(indexService.getSuccessfulIndexOperations()).thenReturn(110L);
        when(indexService.getFailedIndexOperations()).thenReturn(10L);
        when(indexService.getAverageIndexLatencyMs()).thenReturn(15.5);

        Map<String, PostingList> indexMap = new HashMap<>();
        indexMap.put("term1", new com.techatlas.model.PostingList());
        when(invertedIndex.getIndex()).thenReturn(indexMap);

        IndexMetricsResponse response = analyticsService.getIndexMetrics();
        assertEquals(120L, response.indexOperations());
        assertEquals(10L, response.failedOperations());
        assertEquals(15.5, response.averageIndexLatencyMs());
        assertEquals(1, response.vocabularySize());
    }
}
