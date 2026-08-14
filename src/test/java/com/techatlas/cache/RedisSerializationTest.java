package com.techatlas.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techatlas.config.RedisCacheProperties;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.SearchResponse;
import com.techatlas.dto.SearchResult;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisSerializationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCacheProperties properties;
    private RedisCacheService cacheService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        properties = new RedisCacheProperties();
        properties.setEnabled(true);

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cacheService = new RedisCacheService(redisTemplate, properties, objectMapper);
    }

    @Test
    public void testRetrieveDocumentResponseFromLinkedHashMap() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UUID docId = UUID.randomUUID();
        String key = "document:" + docId;

        // Mock JSON Map payload returned by generic Jackson serializer
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("id", docId.toString());
        docMap.put("title", "Test Caching Title");
        docMap.put("content", "Some cached content");
        docMap.put("url", "http://cached-url.com");
        docMap.put("source", "MANUAL");
        docMap.put("category", "Java");
        docMap.put("author", "Author Name");
        docMap.put("language", "en");
        docMap.put("contentHash", "hash123");
        docMap.put("status", "ACTIVE");
        docMap.put("createdAt", "2026-08-14T12:00:00");
        docMap.put("updatedAt", "2026-08-14T12:00:00");
        docMap.put("indexedAt", "2026-08-14T12:05:00");
        docMap.put("metadata", "{\"key\":\"val\"}");

        when(valueOperations.get(key)).thenReturn(docMap);

        Optional<DocumentResponse> result = cacheService.get(key, DocumentResponse.class);

        assertThat(result).isPresent();
        DocumentResponse doc = result.get();
        assertThat(doc.id()).isEqualTo(docId);
        assertThat(doc.title()).isEqualTo("Test Caching Title");
        assertThat(doc.source()).isEqualTo(SourceType.MANUAL);
        assertThat(doc.status()).isEqualTo(DocumentStatus.ACTIVE);
        assertThat(doc.createdAt()).isEqualTo(LocalDateTime.of(2026, 8, 14, 12, 0, 0));
        assertThat(doc.indexedAt()).isEqualTo(LocalDateTime.of(2026, 8, 14, 12, 5, 0));
        assertThat(doc.metadata()).isEqualTo("{\"key\":\"val\"}");
    }

    @Test
    public void testRetrieveSearchResponseFromLinkedHashMap() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String key = "search:hash";
        UUID docId = UUID.randomUUID();

        // Mock nested SearchResult lists within SearchResponse map
        Map<String, Object> resultItemMap = new HashMap<>();
        resultItemMap.put("id", docId.toString());
        resultItemMap.put("title", "SearchResult Title");
        resultItemMap.put("source", "GITHUB");
        resultItemMap.put("url", "http://github.com");
        resultItemMap.put("score", 3.14);
        resultItemMap.put("snippet", "this is a match snippet");
        resultItemMap.put("indexedAt", "2026-08-14T12:05:00");

        Map<String, Object> searchResponseMap = new HashMap<>();
        searchResponseMap.put("query", "spring");
        searchResponseMap.put("totalResults", 1L);
        searchResponseMap.put("results", List.of(resultItemMap));
        searchResponseMap.put("page", 0);
        searchResponseMap.put("size", 10);
        searchResponseMap.put("totalPages", 1);

        when(valueOperations.get(key)).thenReturn(searchResponseMap);

        Optional<SearchResponse> result = cacheService.get(key, SearchResponse.class);

        assertThat(result).isPresent();
        SearchResponse response = result.get();
        assertThat(response.query()).isEqualTo("spring");
        assertThat(response.totalResults()).isEqualTo(1L);
        assertThat(response.results()).hasSize(1);
        
        SearchResult item = response.results().get(0);
        assertThat(item.id()).isEqualTo(docId);
        assertThat(item.source()).isEqualTo(SourceType.GITHUB);
        assertThat(item.score()).isEqualTo(3.14);
        assertThat(item.snippet()).isEqualTo("this is a match snippet");
        assertThat(item.indexedAt()).isEqualTo(LocalDateTime.of(2026, 8, 14, 12, 5, 0));
    }
}
