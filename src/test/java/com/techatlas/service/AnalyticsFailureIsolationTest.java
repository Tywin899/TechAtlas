package com.techatlas.service;

import com.techatlas.config.RedisCacheProperties;
import com.techatlas.config.SearchProperties;
import com.techatlas.config.SyncSchedulerProperties;
import com.techatlas.dto.*;
import com.techatlas.entity.SourceType;
import com.techatlas.index.IndexService;
import com.techatlas.model.InvertedIndex;
import com.techatlas.normalizer.TextNormalizer;
import com.techatlas.repository.*;
import com.techatlas.search.QueryProcessor;
import com.techatlas.search.RankingEngine;
import com.techatlas.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalyticsFailureIsolationTest {

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

    @Mock
    private DocumentService documentService;

    @Mock
    private QueryProcessor queryProcessor;

    @Mock
    private RankingEngine rankingEngine;

    @Mock
    private com.techatlas.stemmer.PorterStemmerAdapter porterStemmerAdapter;

    private SearchProperties searchProperties;
    private RedisCacheProperties redisCacheProperties;
    private AnalyticsService analyticsService;
    private SearchService searchService;

    @BeforeEach
    public void setUp() {
        searchProperties = new SearchProperties();
        searchProperties.getPagination().setDefaultSize(10);
        searchProperties.getPagination().setMaxSize(100);

        redisCacheProperties = new RedisCacheProperties();

        analyticsService = new AnalyticsServiceImpl(
                searchAnalyticsRepository,
                documentRepository,
                sourceSyncRecordRepository,
                indexService,
                sourceSyncService,
                cacheService,
                new SyncSchedulerProperties(),
                textNormalizer,
                invertedIndex
        );

        searchService = new SearchServiceImpl(
                documentService,
                queryProcessor,
                rankingEngine,
                porterStemmerAdapter,
                searchProperties,
                cacheService,
                redisCacheProperties,
                null,
                analyticsService
        );
    }

    @Test
    public void testSearchSucceedsWhenAnalyticsSavingThrowsException() {
        SearchRequest request = new SearchRequest("spring", 0, 10);

        // Setup mock search response path
        when(cacheService.get(anyString(), any())).thenReturn(Optional.empty());
        when(queryProcessor.process("spring")).thenReturn(List.of("spring"));
        when(porterStemmerAdapter.stem(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        
        Map<UUID, Double> scores = new HashMap<>();
        UUID docId = UUID.randomUUID();
        scores.put(docId, 1.5);
        when(rankingEngine.scoreDocuments(any())).thenReturn(scores);

        DocumentResponse docResponse = new DocumentResponse(
                docId, "Title", "spring content snippet here", "http://spring.io", SourceType.MANUAL, null, null, null, "hash", null, null, null, null, null
        );
        when(documentService.retrieve(docId)).thenReturn(docResponse);

        // Force analytics saving to throw exception
        doThrow(new RuntimeException("Database offline")).when(searchAnalyticsRepository).save(any());

        // Perform search
        SearchResponse response = searchService.search(request);

        // Assert search completed successfully despite the database save error
        assertNotNull(response);
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).title()).isEqualTo("Title");
        
        // Confirm repository save was still invoked
        verify(searchAnalyticsRepository, times(1)).save(any());
    }

    @Test
    public void testCacheAnalyticsSucceedsWhenRedisCheckingThrowsException() {
        when(cacheService.getSearchHits()).thenReturn(10L);
        when(cacheService.getSearchMisses()).thenReturn(5L);
        
        // Force redis check connection factory to throw exception
        doThrow(new RuntimeException("Redis connection refused")).when(cacheService).isAvailable();

        CacheAnalyticsResponse response = analyticsService.getCacheAnalytics();

        // Assert metrics are calculated successfully and available is false
        assertNotNull(response);
        assertThat(response.hitRatio()).isEqualTo(10.0 / 15.0);
        assertThat(response.available()).isFalse();
    }
}
