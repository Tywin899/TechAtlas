package com.techatlas.service;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.WikipediaDiscoverRequest;
import com.techatlas.dto.WikipediaDiscoverResponse;
import com.techatlas.dto.WikipediaSyncStatusResponse;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.entity.WikipediaSyncArticle;
import com.techatlas.entity.WikipediaSyncCategory;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.fetcher.wikipedia.WikipediaClient;
import com.techatlas.fetcher.wikipedia.dto.WikipediaCategoryResponse;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.mapper.WikipediaMapper;
import com.techatlas.repository.WikipediaSyncArticleRepository;
import com.techatlas.repository.WikipediaSyncCategoryRepository;
import com.techatlas.util.HashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikipediaServiceTest {

    @Mock
    private WikipediaClient wikipediaClient;

    @Mock
    private DocumentService documentService;

    @Mock
    private WikipediaSyncCategoryRepository wikipediaSyncCategoryRepository;

    @Mock
    private WikipediaSyncArticleRepository wikipediaSyncArticleRepository;

    @Mock
    private SourceSyncService sourceSyncService;

    private WikipediaMapper wikipediaMapper;
    private WikipediaService wikipediaService;

    private WikipediaPageSummary summary;
    private CreateDocumentRequest mappedRequest;
    private DocumentResponse documentResponse;

    @BeforeEach
    void setUp() {
        wikipediaMapper = new WikipediaMapper(new ObjectMapper());
        wikipediaService = new WikipediaServiceImpl(
                wikipediaClient, 
                wikipediaMapper, 
                documentService,
                wikipediaSyncCategoryRepository,
                wikipediaSyncArticleRepository,
                sourceSyncService
        );

        summary = new WikipediaPageSummary(
                "Java",
                "Java programming language.",
                "OOP language",
                12345L,
                "en",
                "1",
                new WikipediaPageSummary.ContentUrls(
                        new WikipediaPageSummary.ContentUrls.Desktop("https://en.wikipedia.org/wiki/Java"),
                        null
                ),
                null
        );

        mappedRequest = wikipediaMapper.toCreateRequest(summary);

        documentResponse = new DocumentResponse(
                UUID.randomUUID(),
                summary.title(),
                summary.extract(),
                "https://en.wikipedia.org/wiki/Java",
                SourceType.WIKIPEDIA,
                null,
                "Wikipedia",
                summary.lang(),
                HashUtil.calculateSha256(summary.extract()),
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                mappedRequest.metadata()
        );
    }

    @Test
    void testFetchSummarySuccess() {
        when(wikipediaClient.fetchPageSummary("Java")).thenReturn(summary);
        WikipediaPageSummary result = wikipediaService.fetchSummary("Java");
        assertNotNull(result);
        assertEquals("Java", result.title());
    }

    @Test
    void testImportArticleSuccess() {
        when(wikipediaClient.fetchPageSummary("Java")).thenReturn(summary);
        
        String hash = HashUtil.calculateSha256(summary.extract());
        when(documentService.findByContentHash(hash)).thenReturn(Optional.empty());
        when(documentService.create(any(CreateDocumentRequest.class))).thenReturn(documentResponse);

        DocumentResponse result = wikipediaService.importArticle("Java");

        assertNotNull(result);
        assertEquals(documentResponse.id(), result.id());
        assertEquals("Java", result.title());
        assertEquals(hash, result.contentHash());
        verify(documentService, times(1)).create(any(CreateDocumentRequest.class));
    }

    @Test
    void testImportArticleDuplicateThrows409() {
        when(wikipediaClient.fetchPageSummary("Java")).thenReturn(summary);
        
        String hash = HashUtil.calculateSha256(summary.extract());
        when(documentService.findByContentHash(hash)).thenReturn(Optional.of(documentResponse));

        assertThrows(DuplicateDocumentException.class, () -> wikipediaService.importArticle("Java"));
        verify(documentService, never()).create(any(CreateDocumentRequest.class));
    }

    @Test
    void testDiscoverArticlesSuccess() {
        WikipediaDiscoverRequest request = new WikipediaDiscoverRequest("Java_programming", 10, 0);

        WikipediaCategoryResponse.WikipediaCategoryMember artMember = 
                new WikipediaCategoryResponse.WikipediaCategoryMember(1, 0, "Java");
        WikipediaCategoryResponse.WikipediaCategoryMember subMember = 
                new WikipediaCategoryResponse.WikipediaCategoryMember(2, 14, "Category:Java_tools");

        WikipediaCategoryResponse response = new WikipediaCategoryResponse(
                null,
                new WikipediaCategoryResponse.WikipediaQuery(List.of(artMember, subMember))
        );

        when(wikipediaClient.fetchCategoryMembers("Java_programming", null)).thenReturn(response);
        when(wikipediaSyncCategoryRepository.findByCategoryName("Java_programming")).thenReturn(Optional.empty());
        when(wikipediaSyncArticleRepository.existsByArticleTitle("Java")).thenReturn(false);

        // Mock import logic calls
        when(wikipediaClient.fetchPageSummary("Java")).thenReturn(summary);
        String hash = HashUtil.calculateSha256(summary.extract());
        when(documentService.findByContentHash(hash)).thenReturn(Optional.empty());
        when(documentService.create(any(CreateDocumentRequest.class))).thenReturn(documentResponse);

        WikipediaDiscoverResponse discoverResponse = wikipediaService.discoverArticles(request);

        assertNotNull(discoverResponse);
        assertEquals("Java_programming", discoverResponse.category());
        assertEquals(1, discoverResponse.articlesDiscovered());
        assertEquals(1, discoverResponse.articlesImported());
        assertEquals(0, discoverResponse.duplicatesSkipped());
        assertEquals(1, discoverResponse.categoriesVisited()); // Only visited starting category

        verify(wikipediaSyncCategoryRepository, times(1)).save(any(WikipediaSyncCategory.class));
        verify(wikipediaSyncArticleRepository, times(1)).save(any(WikipediaSyncArticle.class));
    }

    @Test
    void testDiscoverArticlesRespectsMaxArticlesLimit() {
        WikipediaDiscoverRequest request = new WikipediaDiscoverRequest("Java_programming", 1, 1);

        WikipediaCategoryResponse.WikipediaCategoryMember member1 = 
                new WikipediaCategoryResponse.WikipediaCategoryMember(1, 0, "Java");
        WikipediaCategoryResponse.WikipediaCategoryMember member2 = 
                new WikipediaCategoryResponse.WikipediaCategoryMember(2, 0, "Kotlin");

        WikipediaCategoryResponse response = new WikipediaCategoryResponse(
                null,
                new WikipediaCategoryResponse.WikipediaQuery(List.of(member1, member2))
        );

        when(wikipediaClient.fetchCategoryMembers("Java_programming", null)).thenReturn(response);
        when(wikipediaSyncCategoryRepository.findByCategoryName("Java_programming")).thenReturn(Optional.empty());
        when(wikipediaSyncArticleRepository.existsByArticleTitle("Java")).thenReturn(false);

        // Mock import logic calls for Java
        when(wikipediaClient.fetchPageSummary("Java")).thenReturn(summary);
        String hash = HashUtil.calculateSha256(summary.extract());
        when(documentService.findByContentHash(hash)).thenReturn(Optional.empty());
        when(documentService.create(any(CreateDocumentRequest.class))).thenReturn(documentResponse);

        WikipediaDiscoverResponse discoverResponse = wikipediaService.discoverArticles(request);

        assertNotNull(discoverResponse);
        assertEquals(1, discoverResponse.articlesImported()); // Enforced limit of 1
        verify(documentService, times(1)).create(any(CreateDocumentRequest.class));
    }

    @Test
    void testGetSyncStatus() {
        when(wikipediaSyncCategoryRepository.count()).thenReturn(5L);
        when(wikipediaSyncArticleRepository.count()).thenReturn(20L);
        
        WikipediaSyncCategory cat = new WikipediaSyncCategory("Java", LocalDateTime.now());
        when(wikipediaSyncCategoryRepository.findAll()).thenReturn(List.of(cat));

        WikipediaSyncStatusResponse status = wikipediaService.getSyncStatus();

        assertNotNull(status);
        assertEquals(5L, status.totalSyncedCategories());
        assertEquals(20L, status.totalSyncedArticles());
        assertEquals(1, status.categories().size());
        assertEquals("Java", status.categories().get(0).categoryName());
    }

    @Test
    void testDiscoverArticlesPropagatesCategory() {
        WikipediaDiscoverRequest request = new WikipediaDiscoverRequest("Java_programming", 10, 0);

        WikipediaCategoryResponse.WikipediaCategoryMember artMember = 
                new WikipediaCategoryResponse.WikipediaCategoryMember(1, 0, "Java");

        WikipediaCategoryResponse response = new WikipediaCategoryResponse(
                null,
                new WikipediaCategoryResponse.WikipediaQuery(List.of(artMember))
        );

        when(wikipediaClient.fetchCategoryMembers("Java_programming", null)).thenReturn(response);
        when(wikipediaSyncCategoryRepository.findByCategoryName("Java_programming")).thenReturn(Optional.empty());
        when(wikipediaSyncArticleRepository.existsByArticleTitle("Java")).thenReturn(false);

        when(wikipediaClient.fetchPageSummary("Java")).thenReturn(summary);
        String hash = HashUtil.calculateSha256(summary.extract());
        when(documentService.findByContentHash(hash)).thenReturn(Optional.empty());

        org.mockito.ArgumentCaptor<CreateDocumentRequest> captor = 
                org.mockito.ArgumentCaptor.forClass(CreateDocumentRequest.class);
        when(documentService.create(captor.capture())).thenReturn(documentResponse);

        wikipediaService.discoverArticles(request);

        CreateDocumentRequest capturedRequest = captor.getValue();
        assertNotNull(capturedRequest);
        assertEquals("Java_programming", capturedRequest.category());
    }
}
