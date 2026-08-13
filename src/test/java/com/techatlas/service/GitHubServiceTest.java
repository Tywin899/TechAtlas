package com.techatlas.service;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.GitHubDiscoverRequest;
import com.techatlas.dto.GitHubDiscoverResponse;
import com.techatlas.dto.GitHubSyncStatusResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.GithubSyncRepository;
import com.techatlas.entity.SourceType;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.fetcher.github.GitHubClient;
import com.techatlas.fetcher.github.dto.GitHubOwner;
import com.techatlas.fetcher.github.dto.GitHubReadmeResponse;
import com.techatlas.fetcher.github.dto.GitHubRepoItem;
import com.techatlas.fetcher.github.dto.GitHubSearchResponse;
import com.techatlas.mapper.GitHubMapper;
import com.techatlas.repository.GithubSyncRepositoryRepository;
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
class GitHubServiceTest {

    @Mock
    private GitHubClient githubClient;

    @Mock
    private DocumentService documentService;

    @Mock
    private GithubSyncRepositoryRepository githubSyncRepositoryRepository;

    @Mock
    private SourceSyncService sourceSyncService;

    private GitHubMapper githubMapper;
    private GitHubService githubService;

    private GitHubRepoItem repoItem;
    private GitHubSearchResponse searchResponse;
    private GitHubReadmeResponse readmeResponse;
    private DocumentResponse documentResponse;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        githubMapper = new GitHubMapper(objectMapper);
        githubService = new GitHubServiceImpl(githubClient, githubMapper, documentService, githubSyncRepositoryRepository, sourceSyncService);

        repoItem = new GitHubRepoItem(
                123456L,
                "spring-boot",
                "spring-projects/spring-boot",
                "Spring Boot description",
                "https://github.com/spring-projects/spring-boot",
                100,
                50,
                "Java",
                new GitHubOwner("spring-projects", 9876L),
                List.of("spring", "java"),
                "main",
                null
        );

        searchResponse = new GitHubSearchResponse(1, false, List.of(repoItem));

        readmeResponse = new GitHubReadmeResponse(
                "README.md",
                "README.md",
                "sha123",
                100,
                "SGVsbG8gV29ybGQ=", // Base64 for "Hello World"
                "base64"
        );

        CreateDocumentRequest createRequest = githubMapper.toCreateRequest(repoItem, "Hello World", "spring-boot");

        documentResponse = new DocumentResponse(
                UUID.randomUUID(),
                repoItem.fullName(),
                createRequest.content(),
                repoItem.htmlUrl(),
                SourceType.GITHUB,
                "spring-boot",
                "spring-projects",
                repoItem.language(),
                HashUtil.calculateSha256(createRequest.content()),
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "{}"
        );
    }

    @Test
    void testDiscoverRepositoriesSuccess() {
        GitHubDiscoverRequest request = new GitHubDiscoverRequest("spring-boot", 1);

        when(githubClient.searchRepositories(eq("spring-boot"), eq(1), anyInt())).thenReturn(searchResponse);
        when(githubSyncRepositoryRepository.findByGithubRepoId(123456L)).thenReturn(Optional.empty());
        when(githubClient.fetchReadme("spring-projects", "spring-boot")).thenReturn(readmeResponse);
        when(documentService.create(any(CreateDocumentRequest.class))).thenReturn(documentResponse);

        GitHubDiscoverResponse response = githubService.discoverRepositories(request);

        assertNotNull(response);
        assertEquals("spring-boot", response.query());
        assertEquals(1, response.repositoriesDiscovered());
        assertEquals(1, response.repositoriesImported());
        assertEquals(0, response.duplicatesSkipped());

        verify(githubSyncRepositoryRepository, times(1)).save(any(GithubSyncRepository.class));
    }

    @Test
    void testDiscoverRepositoriesDuplicateSkipped() {
        GitHubDiscoverRequest request = new GitHubDiscoverRequest("spring-boot", 1);

        when(githubClient.searchRepositories(eq("spring-boot"), eq(1), anyInt())).thenReturn(searchResponse);
        
        GithubSyncRepository syncRecord = new GithubSyncRepository(123456L, "spring-projects/spring-boot", LocalDateTime.now(), documentResponse.id());
        when(githubSyncRepositoryRepository.findByGithubRepoId(123456L)).thenReturn(Optional.of(syncRecord));
        when(githubClient.fetchReadme("spring-projects", "spring-boot")).thenReturn(readmeResponse);
        when(documentService.retrieve(documentResponse.id())).thenReturn(documentResponse);

        GitHubDiscoverResponse response = githubService.discoverRepositories(request);

        assertNotNull(response);
        assertEquals(0, response.repositoriesImported());
        assertEquals(1, response.duplicatesSkipped()); // Document content did not change, skipped
        verify(documentService, never()).create(any());
        verify(documentService, never()).update(any(), any());
    }

    @Test
    void testDiscoverRepositoriesUpdatedOnContentChange() {
        GitHubDiscoverRequest request = new GitHubDiscoverRequest("spring-boot", 1);

        when(githubClient.searchRepositories(eq("spring-boot"), eq(1), anyInt())).thenReturn(searchResponse);
        
        GithubSyncRepository syncRecord = new GithubSyncRepository(123456L, "spring-projects/spring-boot", LocalDateTime.now(), documentResponse.id());
        when(githubSyncRepositoryRepository.findByGithubRepoId(123456L)).thenReturn(Optional.of(syncRecord));
        
        GitHubReadmeResponse differentReadme = new GitHubReadmeResponse(
                "README.md", "README.md", "sha456", 100, "RGlmZmVyZW50", "base64" // "Different"
        );
        when(githubClient.fetchReadme("spring-projects", "spring-boot")).thenReturn(differentReadme);
        when(documentService.retrieve(documentResponse.id())).thenReturn(documentResponse);

        GitHubDiscoverResponse response = githubService.discoverRepositories(request);

        assertNotNull(response);
        assertEquals(1, response.repositoriesImported());
        assertEquals(0, response.duplicatesSkipped());
        
        verify(documentService, times(1)).update(eq(documentResponse.id()), any(UpdateDocumentRequest.class));
    }
}
