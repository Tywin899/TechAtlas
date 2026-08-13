package com.techatlas;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.SearchRequest;
import com.techatlas.dto.SearchResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.index.IndexService;
import com.techatlas.model.InvertedIndex;
import com.techatlas.service.DocumentService;
import com.techatlas.service.SearchService;
import com.techatlas.service.WikipediaService;
import com.techatlas.dto.WikipediaDiscoverRequest;
import com.techatlas.dto.WikipediaDiscoverResponse;
import com.techatlas.fetcher.wikipedia.WikipediaClient;
import com.techatlas.fetcher.wikipedia.dto.WikipediaCategoryResponse;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.service.GitHubService;
import com.techatlas.dto.GitHubDiscoverRequest;
import com.techatlas.dto.GitHubDiscoverResponse;
import com.techatlas.fetcher.github.GitHubClient;
import com.techatlas.fetcher.github.dto.GitHubSearchResponse;
import com.techatlas.fetcher.github.dto.GitHubRepoItem;
import com.techatlas.fetcher.github.dto.GitHubOwner;
import com.techatlas.fetcher.github.dto.GitHubReadmeResponse;
import com.techatlas.dto.SourceSyncResponse;
import com.techatlas.service.SourceSyncService;
import com.techatlas.service.StackOverflowService;
import com.techatlas.dto.StackOverflowDiscoverRequest;
import com.techatlas.dto.StackOverflowDiscoverResponse;
import com.techatlas.fetcher.stackoverflow.StackOverflowClient;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowResponseWrapper;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class TechAtlasApplicationTests {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private IndexService indexService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private InvertedIndex invertedIndex;

    @Autowired
    private WikipediaService wikipediaService;

    @MockBean
    private WikipediaClient wikipediaClient;

    @Autowired
    private GitHubService githubService;

    @MockBean
    private GitHubClient githubClient;

    @Autowired
    private StackOverflowService stackOverflowService;

    @MockBean
    private StackOverflowClient stackOverflowClient;

    @Autowired
    private SourceSyncService sourceSyncService;

    @Autowired
    private com.techatlas.repository.SourceSyncRecordRepository sourceSyncRecordRepository;

    @BeforeEach
    void setUp() {
        invertedIndex.clear();
    }

    @Test
    void contextLoads() {
        // Verification test that the Spring application context starts up correctly.
    }

    @Test
    void testEndToEndIncrementalIndexingAndSearchFlow() {
        // 1. Create document A
        CreateDocumentRequest createRequest = new CreateDocumentRequest(
                "Java Intro",
                "Java is a popular programming language.",
                "https://example.com/java",
                SourceType.MANUAL,
                "programming",
                "Author",
                "en",
                null
        );
        DocumentResponse docA = documentService.create(createRequest);
        assertThat(docA.status().name()).isEqualTo("PENDING_INDEX");

        // Index document A
        indexService.indexDocument(docA.id());
        
        // Search old term -> A found
        SearchResponse searchResponse = searchService.search(new SearchRequest("java", 0, 10));
        assertThat(searchResponse.totalResults()).isEqualTo(1);
        assertThat(searchResponse.results().get(0).id()).isEqualTo(docA.id());

        // 2. Update document A
        UpdateDocumentRequest updateRequest = new UpdateDocumentRequest(
                "Java Intro",
                "Python is another language.",
                "https://example.com/java",
                SourceType.MANUAL,
                "programming",
                "Author",
                "en",
                null
        );
        DocumentResponse updatedDoc = documentService.update(docA.id(), updateRequest);
        assertThat(updatedDoc.status().name()).isEqualTo("PENDING_INDEX");

        // Verify it was evicted from index upon update
        SearchResponse searchOldBeforeReindex = searchService.search(new SearchRequest("java", 0, 10));
        assertThat(searchOldBeforeReindex.totalResults()).isEqualTo(0);

        // Re-index document A
        indexService.indexDocument(docA.id());

        // Search old term -> A NOT found
        SearchResponse searchOldAfterReindex = searchService.search(new SearchRequest("java", 0, 10));
        assertThat(searchOldAfterReindex.totalResults()).isEqualTo(0);

        // Search new term -> A found
        SearchResponse searchNew = searchService.search(new SearchRequest("python", 0, 10));
        assertThat(searchNew.totalResults()).isEqualTo(1);
        assertThat(searchNew.results().get(0).id()).isEqualTo(docA.id());

        // 3. Delete document A
        documentService.delete(docA.id());

        // Search new term -> A NOT found
        SearchResponse searchAfterDelete = searchService.search(new SearchRequest("python", 0, 10));
        assertThat(searchAfterDelete.totalResults()).isEqualTo(0);
    }

    @Test
    void testWikipediaCategoryDiscoveryAndSearchFlow() {
        // Mock category members endpoint
        WikipediaCategoryResponse.WikipediaCategoryMember member = 
                new WikipediaCategoryResponse.WikipediaCategoryMember(1, 0, "Java programming language");
        
        WikipediaCategoryResponse categoryResponse = new WikipediaCategoryResponse(
                null,
                new WikipediaCategoryResponse.WikipediaQuery(List.of(member))
        );

        when(wikipediaClient.fetchCategoryMembers(eq("Java_programming"), any())).thenReturn(categoryResponse);

        // Mock summary fetching for the article
        WikipediaPageSummary pageSummary = new WikipediaPageSummary(
                "Java programming language",
                "Java is a high-level language.",
                "OOP language",
                12345L,
                "en",
                "1",
                new WikipediaPageSummary.ContentUrls(
                        new WikipediaPageSummary.ContentUrls.Desktop("https://en.wikipedia.org/wiki/Java_programming_language"),
                        null
                ),
                null
        );
        when(wikipediaClient.fetchPageSummary("Java programming language")).thenReturn(pageSummary);

        // Run discovery
        WikipediaDiscoverResponse discoverResponse = wikipediaService.discoverArticles(
                new WikipediaDiscoverRequest("Java_programming", 5, 0)
        );

        assertThat(discoverResponse.articlesImported()).isEqualTo(1);
        assertThat(discoverResponse.duplicatesSkipped()).isEqualTo(0);

        // Retrieve the document
        List<DocumentResponse> documents = documentService.listAll();
        assertThat(documents).isNotEmpty();
        DocumentResponse doc = documents.stream()
                .filter(d -> d.title().equals("Java programming language"))
                .findFirst()
                .orElseThrow();
        assertThat(doc.status().name()).isEqualTo("PENDING_INDEX");
        assertThat(doc.category()).isEqualTo("Java_programming");

        // Index it
        indexService.indexDocument(doc.id());

        // Search it
        SearchResponse searchResponse = searchService.search(new SearchRequest("high-level", 0, 10));
        assertThat(searchResponse.totalResults()).isEqualTo(1);
        assertThat(searchResponse.results().get(0).title()).isEqualTo("Java programming language");
    }

    @Test
    void testGitHubRepositoryDiscoveryAndSearchFlow() {
        GitHubRepoItem repoItem = new GitHubRepoItem(
                999999L,
                "spring-boot",
                "spring-projects/spring-boot",
                "Spring Boot framework",
                "https://github.com/spring-projects/spring-boot",
                100,
                50,
                "Java",
                new GitHubOwner("spring-projects", 9876L),
                List.of("spring", "java"),
                "main",
                null
        );

        GitHubSearchResponse searchResp = new GitHubSearchResponse(1, false, List.of(repoItem));
        when(githubClient.searchRepositories(eq("spring-boot"), eq(1), anyInt())).thenReturn(searchResp);

        GitHubReadmeResponse readmeResp = new GitHubReadmeResponse(
                "README.md",
                "README.md",
                "sha123",
                100,
                "U3ByaW5nIEJvb3QgaXMgb3V0c3RhbmRpbmcu", // Base64 for "Spring Boot is outstanding."
                "base64"
        );
        when(githubClient.fetchReadme("spring-projects", "spring-boot")).thenReturn(readmeResp);

        GitHubDiscoverResponse discoverResponse = githubService.discoverRepositories(
                new GitHubDiscoverRequest("spring-boot", 5)
        );

        assertThat(discoverResponse.repositoriesImported()).isEqualTo(1);
        assertThat(discoverResponse.duplicatesSkipped()).isEqualTo(0);

        List<DocumentResponse> documents = documentService.listAll();
        assertThat(documents).isNotEmpty();
        DocumentResponse doc = documents.stream()
                .filter(d -> d.title().equals("spring-projects/spring-boot"))
                .findFirst()
                .orElseThrow();
        assertThat(doc.status().name()).isEqualTo("PENDING_INDEX");
        assertThat(doc.category()).isEqualTo("spring-boot");
        assertThat(doc.source().name()).isEqualTo("GITHUB");

        indexService.indexDocument(doc.id());

        SearchResponse searchResponseResults = searchService.search(new SearchRequest("outstanding", 0, 10));
        assertThat(searchResponseResults.totalResults()).isEqualTo(1);
        assertThat(searchResponseResults.results().get(0).title()).isEqualTo("spring-projects/spring-boot");
    }

    @Test
    void testStackOverflowQuestionDiscoveryAndSearchFlow() {
        StackOverflowQuestionItem question = new StackOverflowQuestionItem(
                998877L,
                "Spring Boot Context Refreshed Event",
                "<p>How to use context refreshed event in Spring Boot?</p>",
                "https://stackoverflow.com/questions/998877",
                15,
                List.of("spring-boot", "java"),
                new StackOverflowOwner("EventMaster", 77L),
                true,
                1,
                6655L,
                1628859600L
        );

        StackOverflowResponseWrapper<StackOverflowQuestionItem> searchResp = new StackOverflowResponseWrapper<>(
                List.of(question), false, 100, 100
        );
        when(stackOverflowClient.searchQuestions(eq("refreshed"), any(), eq(1), anyInt())).thenReturn(searchResp);

        StackOverflowAnswerItem answer = new StackOverflowAnswerItem(
                6655L,
                "<p>Simply implement ApplicationListener for ContextRefreshedEvent, it is magnificent.</p>",
                25,
                true,
                new StackOverflowOwner("SpringGuru", 66L)
        );
        StackOverflowResponseWrapper<StackOverflowAnswerItem> answerResp = new StackOverflowResponseWrapper<>(
                List.of(answer), false, 100, 100
        );
        when(stackOverflowClient.fetchAnswers(998877L)).thenReturn(answerResp);

        StackOverflowDiscoverResponse discoverResponse = stackOverflowService.discoverQuestions(
                new StackOverflowDiscoverRequest("refreshed", List.of("spring-boot"), 5)
        );

        assertThat(discoverResponse.questionsImported()).isEqualTo(1);
        assertThat(discoverResponse.duplicatesSkipped()).isEqualTo(0);

        List<DocumentResponse> documents = documentService.listAll();
        assertThat(documents).isNotEmpty();
        DocumentResponse doc = documents.stream()
                .filter(d -> d.title().equals("Spring Boot Context Refreshed Event"))
                .findFirst()
                .orElseThrow();
        assertThat(doc.status().name()).isEqualTo("PENDING_INDEX");
        assertThat(doc.category()).isEqualTo("refreshed");
        assertThat(doc.source().name()).isEqualTo("STACKOVERFLOW");

        indexService.indexDocument(doc.id());

        SearchResponse searchResponseResults = searchService.search(new SearchRequest("magnificent", 0, 10));
        assertThat(searchResponseResults.totalResults()).isEqualTo(1);
        assertThat(searchResponseResults.results().get(0).title()).isEqualTo("Spring Boot Context Refreshed Event");
    }

    @Test
    void testE2ESourceSynchronizationFlow() {
        sourceSyncRecordRepository.deleteAll();

        WikipediaPageSummary summary = new WikipediaPageSummary(
                "Adoptium Project",
                "Adoptium is a project under Eclipse Foundation.",
                "Adoptium description",
                98765L,
                "en",
                "rev10",
                new WikipediaPageSummary.ContentUrls(
                        new WikipediaPageSummary.ContentUrls.Desktop("https://en.wikipedia.org/wiki/Adoptium_Project"),
                        new WikipediaPageSummary.ContentUrls.Mobile("https://en.wikipedia.org/wiki/Adoptium_Project")
                ),
                null
        );
        when(wikipediaClient.fetchPageSummary("Adoptium Project")).thenReturn(summary);

        DocumentResponse doc = wikipediaService.importArticle("Adoptium Project", "java");
        assertThat(doc).isNotNull();

        var optRecord = sourceSyncRecordRepository.findBySourceAndExternalId(SourceType.WIKIPEDIA, "98765");
        assertThat(optRecord).isPresent();
        var record = optRecord.get();
        assertThat(record.getExternalRevision()).isEqualTo("rev10");
        assertThat(record.getStatus().name()).isEqualTo("SYNCED");

        indexService.indexDocument(doc.id());
        SearchResponse initialSearch = searchService.search(new SearchRequest("Eclipse", 0, 10));
        assertThat(initialSearch.totalResults()).isEqualTo(1);

        SourceSyncResponse syncResponse = sourceSyncService.syncSource(SourceType.WIKIPEDIA);
        assertThat(syncResponse.checked()).isEqualTo(1);
        assertThat(syncResponse.unchangedResources()).isEqualTo(1);
        assertThat(syncResponse.changedResources()).isEqualTo(0);

        WikipediaPageSummary updatedSummary = new WikipediaPageSummary(
                "Adoptium Project",
                "Adoptium is a project under Eclipse Foundation. Now with OpenJDK support.",
                "Adoptium description",
                98765L,
                "en",
                "rev11",
                new WikipediaPageSummary.ContentUrls(
                        new WikipediaPageSummary.ContentUrls.Desktop("https://en.wikipedia.org/wiki/Adoptium_Project"),
                        new WikipediaPageSummary.ContentUrls.Mobile("https://en.wikipedia.org/wiki/Adoptium_Project")
                ),
                null
        );
        when(wikipediaClient.fetchPageSummary("Adoptium Project")).thenReturn(updatedSummary);

        SourceSyncResponse syncResponse2 = sourceSyncService.syncSource(SourceType.WIKIPEDIA);
        assertThat(syncResponse2.checked()).isEqualTo(1);
        assertThat(syncResponse2.changedResources()).isEqualTo(1);

        SearchResponse updatedSearch = searchService.search(new SearchRequest("OpenJDK", 0, 10));
        assertThat(updatedSearch.totalResults()).isEqualTo(1);
    }
}
