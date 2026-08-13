package com.techatlas.service;

import com.techatlas.config.SearchProperties;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.SearchRequest;
import com.techatlas.dto.SearchResponse;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.search.QueryProcessor;
import com.techatlas.search.RankingEngine;
import com.techatlas.stemmer.PorterStemmerAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private QueryProcessor queryProcessor;

    @Mock
    private RankingEngine rankingEngine;

    @Mock
    private PorterStemmerAdapter porterStemmerAdapter;

    @Mock
    private com.techatlas.cache.CacheService cacheService;

    private SearchProperties searchProperties;
    private SearchService searchService;

    @BeforeEach
    public void setUp() {
        searchProperties = new SearchProperties();
        searchProperties.getPagination().setDefaultSize(10);
        searchProperties.getPagination().setMaxSize(100);

        searchService = new SearchServiceImpl(
                documentService,
                queryProcessor,
                rankingEngine,
                porterStemmerAdapter,
                searchProperties,
                cacheService,
                new com.techatlas.config.RedisCacheProperties()
        );
    }

    @Test
    public void testSearchSuccess() {
        UUID docId = UUID.randomUUID();
        SearchRequest request = new SearchRequest("spring framework", 0, 10);

        when(queryProcessor.process("spring framework")).thenReturn(List.of("spring", "framework"));
        when(rankingEngine.scoreDocuments(List.of("spring", "framework"))).thenReturn(Map.of(docId, 5.91));

        DocumentResponse doc = new DocumentResponse(
                docId,
                "Spring Framework",
                "The Spring Framework is an inversion of control container.",
                "http://spring.io",
                SourceType.WIKIPEDIA,
                null,
                "Wikipedia",
                "en",
                "hash",
                DocumentStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
        when(documentService.retrieve(docId)).thenReturn(doc);
        when(porterStemmerAdapter.stem(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        SearchResponse response = searchService.search(request);

        assertThat(response).isNotNull();
        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).title()).isEqualTo("Spring Framework");
        assertThat(response.results().get(0).score()).isEqualTo(5.91);
    }

    @Test
    public void testSearchValidationEmptyQuery() {
        SearchRequest request = new SearchRequest("", 0, 10);
        assertThrows(IllegalArgumentException.class, () -> searchService.search(request));
    }

    @Test
    public void testSearchValidationInvalidPage() {
        SearchRequest request = new SearchRequest("java", -1, 10);
        assertThrows(IllegalArgumentException.class, () -> searchService.search(request));
    }
}
