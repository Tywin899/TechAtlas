package com.techatlas.service;

import com.techatlas.cache.CacheService;
import com.techatlas.config.RedisCacheProperties;
import com.techatlas.config.SearchProperties;
import com.techatlas.dto.SearchRequest;
import com.techatlas.dto.SearchResponse;
import com.techatlas.search.QueryProcessor;
import com.techatlas.search.RankingEngine;
import com.techatlas.stemmer.PorterStemmerAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchCacheTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private QueryProcessor queryProcessor;

    @Mock
    private RankingEngine rankingEngine;

    @Mock
    private PorterStemmerAdapter porterStemmerAdapter;

    @Mock
    private CacheService cacheService;

    private SearchProperties searchProperties;
    private RedisCacheProperties redisCacheProperties;
    private SearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        searchProperties = new SearchProperties();
        searchProperties.getPagination().setDefaultSize(10);
        searchProperties.getPagination().setMaxSize(100);

        redisCacheProperties = new RedisCacheProperties();

        searchService = new SearchServiceImpl(
                documentService,
                queryProcessor,
                rankingEngine,
                porterStemmerAdapter,
                searchProperties,
                cacheService,
                redisCacheProperties
        );
    }

    @Test
    void testSearchCacheHit() {
        SearchRequest request = new SearchRequest("spring", 0, 10);
        SearchResponse cachedResponse = new SearchResponse("spring", 0L, Collections.emptyList(), 0, 10, 0);

        when(cacheService.get(anyString(), eq(SearchResponse.class))).thenReturn(Optional.of(cachedResponse));

        SearchResponse result = searchService.search(request);

        assertEquals(cachedResponse, result);
        verify(cacheService, times(1)).incrementSearchHits();
        verifyNoInteractions(queryProcessor);
    }

    @Test
    void testSearchCacheMissEmptyResult() {
        SearchRequest request = new SearchRequest("spring", 0, 10);
        SearchResponse expectedResponse = new SearchResponse("spring", 0L, Collections.emptyList(), 0, 10, 0);

        when(cacheService.get(anyString(), eq(SearchResponse.class))).thenReturn(Optional.empty());
        when(queryProcessor.process("spring")).thenReturn(Collections.emptyList());

        SearchResponse result = searchService.search(request);

        assertEquals(expectedResponse, result);
        verify(cacheService, times(1)).incrementSearchMisses();
        verify(cacheService, times(1)).put(anyString(), eq(expectedResponse), anyLong(), eq(TimeUnit.SECONDS));
    }
}
