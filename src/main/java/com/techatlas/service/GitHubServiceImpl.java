package com.techatlas.service;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.GitHubDiscoverRequest;
import com.techatlas.dto.GitHubDiscoverResponse;
import com.techatlas.dto.GitHubSyncStatusResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.GithubSyncRepository;
import com.techatlas.exception.DocumentNotFoundException;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.fetcher.github.GitHubClient;
import com.techatlas.fetcher.github.dto.GitHubReadmeResponse;
import com.techatlas.fetcher.github.dto.GitHubRepoItem;
import com.techatlas.fetcher.github.dto.GitHubSearchResponse;
import com.techatlas.mapper.GitHubMapper;
import com.techatlas.repository.GithubSyncRepositoryRepository;
import com.techatlas.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GitHubServiceImpl implements GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubServiceImpl.class);

    private final GitHubClient githubClient;
    private final GitHubMapper githubMapper;
    private final DocumentService documentService;
    private final GithubSyncRepositoryRepository githubSyncRepositoryRepository;
    private final SourceSyncService sourceSyncService;

    public GitHubServiceImpl(
            GitHubClient githubClient,
            GitHubMapper githubMapper,
            DocumentService documentService,
            GithubSyncRepositoryRepository githubSyncRepositoryRepository,
            SourceSyncService sourceSyncService) {
        this.githubClient = githubClient;
        this.githubMapper = githubMapper;
        this.documentService = documentService;
        this.githubSyncRepositoryRepository = githubSyncRepositoryRepository;
        this.sourceSyncService = sourceSyncService;
    }

    private void registerSync(GitHubRepoItem item, String readmeSha, String contentHash, UUID docId) {
        if (docId != null) {
            sourceSyncService.createOrUpdateSyncRecord(
                    com.techatlas.entity.SourceType.GITHUB,
                    item.id().toString(),
                    readmeSha,
                    contentHash,
                    docId
            );
        }
    }

    @Override
    @Transactional
    public GitHubDiscoverResponse discoverRepositories(GitHubDiscoverRequest request) {
        String query = request.query().trim();
        int maxRepositories = request.maxRepositories();

        int repositoriesDiscovered = 0;
        int repositoriesImported = 0;
        int duplicatesSkipped = 0;

        int page = 1;
        int pageSize = Math.min(30, maxRepositories);
        boolean hasMoreResults = true;

        while (hasMoreResults && repositoriesImported < maxRepositories) {
            GitHubSearchResponse searchResponse;
            try {
                searchResponse = githubClient.searchRepositories(query, page, pageSize);
            } catch (Exception e) {
                logger.error("Failed to fetch GitHub search results for query [{}]: {}", query, e.getMessage());
                throw e;
            }

            if (searchResponse == null || searchResponse.items() == null || searchResponse.items().isEmpty()) {
                break;
            }

            List<GitHubRepoItem> items = searchResponse.items();
            for (GitHubRepoItem item : items) {
                if (repositoriesImported >= maxRepositories) {
                    break;
                }

                repositoriesDiscovered++;

                try {
                    Optional<GithubSyncRepository> existingSync = githubSyncRepositoryRepository.findByGithubRepoId(item.id());

                    String readmeContent = null;
                    GitHubReadmeResponse readmeResponse = null;
                    try {
                        readmeResponse = githubClient.fetchReadme(item.owner().login(), item.name());
                        if (readmeResponse != null && readmeResponse.content() != null && "base64".equalsIgnoreCase(readmeResponse.encoding())) {
                            byte[] decodedBytes = Base64.getMimeDecoder().decode(readmeResponse.content().replaceAll("\\s", ""));
                            readmeContent = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fetch README for {}/{}: {}", item.owner().login(), item.name(), e.getMessage());
                    }

                    String readmeSha = readmeResponse != null ? readmeResponse.sha() : null;

                    CreateDocumentRequest createRequest = githubMapper.toCreateRequest(item, readmeContent, query);
                    String newContentHash = HashUtil.calculateSha256(createRequest.content());

                    if (existingSync.isPresent()) {
                        GithubSyncRepository sync = existingSync.get();
                        UUID existingDocId = sync.getDocumentId();

                        if (existingDocId != null) {
                            try {
                                DocumentResponse existingDoc = documentService.retrieve(existingDocId);
                                if (existingDoc.contentHash().equals(newContentHash)) {
                                    duplicatesSkipped++;
                                    continue;
                                } else {
                                    UpdateDocumentRequest updateRequest = new UpdateDocumentRequest(
                                            createRequest.title(),
                                            createRequest.content(),
                                            createRequest.url(),
                                            createRequest.source(),
                                            createRequest.category(),
                                            createRequest.author(),
                                            createRequest.language(),
                                            createRequest.metadata()
                                    );
                                    documentService.update(existingDocId, updateRequest);
                                    repositoriesImported++;

                                    registerSync(item, readmeSha, newContentHash, existingDocId);

                                    sync.setLastSyncedAt(LocalDateTime.now());
                                    sync.setRepositoryName(item.fullName());
                                    githubSyncRepositoryRepository.save(sync);
                                }
                            } catch (DocumentNotFoundException e) {
                                DocumentResponse created = documentService.create(createRequest);
                                repositoriesImported++;

                                registerSync(item, readmeSha, newContentHash, created.id());

                                sync.setDocumentId(created.id());
                                sync.setLastSyncedAt(LocalDateTime.now());
                                sync.setRepositoryName(item.fullName());
                                githubSyncRepositoryRepository.save(sync);
                            }
                        } else {
                            try {
                                DocumentResponse created = documentService.create(createRequest);
                                repositoriesImported++;

                                registerSync(item, readmeSha, newContentHash, created.id());

                                sync.setDocumentId(created.id());
                                sync.setLastSyncedAt(LocalDateTime.now());
                                sync.setRepositoryName(item.fullName());
                                githubSyncRepositoryRepository.save(sync);
                            } catch (DuplicateDocumentException e) {
                                duplicatesSkipped++;
                            }
                        }
                    } else {
                        try {
                             DocumentResponse created = documentService.create(createRequest);
                             repositoriesImported++;

                             registerSync(item, readmeSha, newContentHash, created.id());

                             GithubSyncRepository sync = new GithubSyncRepository(
                                     item.id(),
                                     item.fullName(),
                                     LocalDateTime.now(),
                                     created.id()
                               );
                             githubSyncRepositoryRepository.save(sync);
                         } catch (DuplicateDocumentException e) {
                             duplicatesSkipped++;
                            GithubSyncRepository sync = new GithubSyncRepository(
                                    item.id(),
                                    item.fullName(),
                                    LocalDateTime.now(),
                                    null
                            );
                            githubSyncRepositoryRepository.save(sync);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error importing repository [{}]: {}", item.fullName(), e.getMessage());
                }
            }

            if (searchResponse.items().size() < pageSize) {
                hasMoreResults = false;
            } else {
                page++;
            }
        }

        return new GitHubDiscoverResponse(
                query,
                repositoriesDiscovered,
                repositoriesImported,
                duplicatesSkipped
        );
    }

    @Override
    public GitHubSyncStatusResponse getSyncStatus() {
        long totalRepos = githubSyncRepositoryRepository.count();
        List<GitHubSyncStatusResponse.RepositorySyncInfo> repos = githubSyncRepositoryRepository.findAll().stream()
                .map(r -> new GitHubSyncStatusResponse.RepositorySyncInfo(
                        r.getGithubRepoId(),
                        r.getRepositoryName(),
                        r.getLastSyncedAt()
                ))
                .toList();
        return new GitHubSyncStatusResponse(totalRepos, repos);
    }
}
