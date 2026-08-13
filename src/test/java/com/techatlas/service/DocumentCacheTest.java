package com.techatlas.service;

import com.techatlas.cache.CacheService;
import com.techatlas.config.RedisCacheProperties;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.Document;
import com.techatlas.entity.SourceType;
import com.techatlas.mapper.DocumentMapper;
import com.techatlas.model.InvertedIndex;
import com.techatlas.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentCacheTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private InvertedIndex invertedIndex;

    @Mock
    private CacheService cacheService;

    private RedisCacheProperties redisCacheProperties;
    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        redisCacheProperties = new RedisCacheProperties();
        documentService = new DocumentServiceImpl(
                documentRepository,
                documentMapper,
                invertedIndex,
                cacheService,
                redisCacheProperties
        );
    }

    @Test
    void testRetrieveCacheHit() {
        UUID docId = UUID.randomUUID();
        DocumentResponse response = new DocumentResponse(
                docId, "Title", "content", "http://url", SourceType.MANUAL, null, null, null, "hash", null, null, null, null, null
        );

        when(cacheService.get("document:" + docId)).thenReturn(Optional.of(response));

        DocumentResponse result = documentService.retrieve(docId);

        assertEquals(response, result);
        verify(cacheService, times(1)).incrementDocumentHits();
        verifyNoInteractions(documentRepository);
    }

    @Test
    void testRetrieveCacheMiss() {
        UUID docId = UUID.randomUUID();
        Document document = new Document();
        document.setId(docId);
        DocumentResponse response = new DocumentResponse(
                docId, "Title", "content", "http://url", SourceType.MANUAL, null, null, null, "hash", null, null, null, null, null
        );

        when(cacheService.get("document:" + docId)).thenReturn(Optional.empty());
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentMapper.toResponse(document)).thenReturn(response);

        DocumentResponse result = documentService.retrieve(docId);

        assertEquals(response, result);
        verify(cacheService, times(1)).incrementDocumentMisses();
        verify(cacheService, times(1)).put(eq("document:" + docId), eq(response), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void testUpdateEvictsCache() {
        UUID docId = UUID.randomUUID();
        Document document = new Document();
        document.setId(docId);
        document.setContentHash("old-hash");

        UpdateDocumentRequest request = new UpdateDocumentRequest("Title", "New Content", "http://url", SourceType.MANUAL, null, null, null, null);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentRepository.saveAndFlush(any())).thenReturn(document);

        documentService.update(docId, request);

        verify(cacheService, times(1)).evict("document:" + docId);
        verify(cacheService, times(1)).clearAllSearchCaches();
    }
}
