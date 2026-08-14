package com.techatlas.service;

import com.techatlas.cache.CacheService;
import com.techatlas.config.RedisCacheProperties;
import com.techatlas.config.SearchProperties;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.SearchRequest;
import com.techatlas.dto.SearchResponse;
import com.techatlas.entity.Document;
import com.techatlas.entity.SourceType;
import com.techatlas.mapper.DocumentMapper;
import com.techatlas.model.InvertedIndex;
import com.techatlas.repository.DocumentRepository;
import com.techatlas.search.QueryProcessor;
import com.techatlas.search.RankingEngine;
import com.techatlas.stemmer.PorterStemmerAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisFailureTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private InvertedIndex invertedIndex;

    @Mock
    private QueryProcessor queryProcessor;

    @Mock
    private RankingEngine rankingEngine;

    @Mock
    private PorterStemmerAdapter porterStemmerAdapter;

    @Test
    void testRetrieveWorksOnRedisFailure() {
        UUID docId = UUID.randomUUID();
        Document document = new Document();
        document.setId(docId);
        DocumentResponse response = new DocumentResponse(
                docId, "Title", "content", "http://url", SourceType.MANUAL, null, null, null, "hash", null, null, null, null, null
        );

        when(cacheService.get(anyString(), any())).thenReturn(Optional.empty());
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentMapper.toResponse(document)).thenReturn(response);

        DocumentServiceImpl documentService = new DocumentServiceImpl(
                documentRepository, documentMapper, invertedIndex, cacheService, new RedisCacheProperties()
        );

        DocumentResponse result = documentService.retrieve(docId);

        assertNotNull(result);
        assertEquals(response, result);
        verify(cacheService, times(1)).incrementDocumentMisses();
    }

    @Test
    void testSearchWorksOnRedisFailure() {
        SearchRequest request = new SearchRequest("spring", 0, 10);

        when(cacheService.get(anyString(), any())).thenReturn(Optional.empty());
        when(queryProcessor.process("spring")).thenReturn(Collections.emptyList());

        SearchProperties searchProperties = new SearchProperties();
        searchProperties.getPagination().setDefaultSize(10);
        searchProperties.getPagination().setMaxSize(100);

        SearchServiceImpl searchService = new SearchServiceImpl(
                mock(DocumentService.class), queryProcessor, rankingEngine, porterStemmerAdapter, searchProperties, cacheService, new RedisCacheProperties()
        );

        SearchResponse result = searchService.search(request);

        assertNotNull(result);
        assertEquals("spring", result.query());
        verify(cacheService, times(1)).incrementSearchMisses();
    }
}
