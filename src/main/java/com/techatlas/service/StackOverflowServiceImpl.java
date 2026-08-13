package com.techatlas.service;

import com.techatlas.config.StackOverflowProperties;
import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.StackOverflowDiscoverRequest;
import com.techatlas.dto.StackOverflowDiscoverResponse;
import com.techatlas.dto.StackOverflowSyncStatusResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.StackOverflowSyncQuestion;
import com.techatlas.exception.DocumentNotFoundException;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.fetcher.stackoverflow.StackOverflowClient;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowResponseWrapper;
import com.techatlas.mapper.StackOverflowMapper;
import com.techatlas.repository.StackOverflowSyncQuestionRepository;
import com.techatlas.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StackOverflowServiceImpl implements StackOverflowService {

    private static final Logger logger = LoggerFactory.getLogger(StackOverflowServiceImpl.class);

    private final StackOverflowClient client;
    private final StackOverflowMapper mapper;
    private final DocumentService documentService;
    private final StackOverflowSyncQuestionRepository repository;
    private final StackOverflowProperties properties;
    private final SourceSyncService sourceSyncService;

    public StackOverflowServiceImpl(
            StackOverflowClient client,
            StackOverflowMapper mapper,
            DocumentService documentService,
            StackOverflowSyncQuestionRepository repository,
            StackOverflowProperties properties,
            SourceSyncService sourceSyncService) {
        this.client = client;
        this.mapper = mapper;
        this.documentService = documentService;
        this.repository = repository;
        this.properties = properties;
        this.sourceSyncService = sourceSyncService;
    }

    private void registerSync(StackOverflowQuestionItem item, String contentHash, UUID docId) {
        if (docId != null) {
            String revision = item.lastActivityDate() != null ? item.lastActivityDate().toString() : null;
            sourceSyncService.createOrUpdateSyncRecord(
                    com.techatlas.entity.SourceType.STACKOVERFLOW,
                    item.questionId().toString(),
                    revision,
                    contentHash,
                    docId
            );
        }
    }

    @Override
    @Transactional
    public StackOverflowDiscoverResponse discoverQuestions(StackOverflowDiscoverRequest request) {
        String query = request.query().trim();
        List<String> tags = request.tags();
        int maxQuestions = request.maxQuestions();

        int questionsDiscovered = 0;
        int questionsImported = 0;
        int duplicatesSkipped = 0;

        int page = 1;
        int pageSize = Math.min(properties.getDefaultPageSize(), maxQuestions);
        boolean hasMoreResults = true;

        while (hasMoreResults && questionsImported < maxQuestions) {
            StackOverflowResponseWrapper<StackOverflowQuestionItem> searchResponse;
            try {
                searchResponse = client.searchQuestions(query, tags, page, pageSize);
            } catch (Exception e) {
                logger.error("Failed to fetch Stack Overflow search results for query [{}]: {}", query, e.getMessage());
                throw e;
            }

            if (searchResponse == null || searchResponse.items() == null || searchResponse.items().isEmpty()) {
                break;
            }

            List<StackOverflowQuestionItem> items = searchResponse.items();
            for (StackOverflowQuestionItem item : items) {
                if (questionsImported >= maxQuestions) {
                    break;
                }

                questionsDiscovered++;

                try {
                    Optional<StackOverflowSyncQuestion> existingSync = repository.findByQuestionId(item.questionId());

                    List<StackOverflowAnswerItem> sortedAnswers = new ArrayList<>();
                    if (item.answerCount() > 0) {
                        try {
                            StackOverflowResponseWrapper<StackOverflowAnswerItem> answersResponse = client.fetchAnswers(item.questionId());
                            if (answersResponse != null && answersResponse.items() != null) {
                                List<StackOverflowAnswerItem> allAnswers = answersResponse.items();
                                
                                List<StackOverflowAnswerItem> accepted = allAnswers.stream()
                                        .filter(StackOverflowAnswerItem::isAccepted)
                                        .toList();
                                
                                List<StackOverflowAnswerItem> others = allAnswers.stream()
                                        .filter(a -> !a.isAccepted())
                                        .sorted((a1, a2) -> Integer.compare(a2.score(), a1.score()))
                                        .toList();
                                
                                sortedAnswers.addAll(accepted);
                                sortedAnswers.addAll(others);
                                
                                int maxAnswers = properties.getMaxAnswersPerQuestion();
                                if (sortedAnswers.size() > maxAnswers) {
                                    sortedAnswers = sortedAnswers.subList(0, maxAnswers);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to fetch answers for Stack Overflow question [{}]: {}", item.questionId(), e.getMessage());
                        }
                    }

                    CreateDocumentRequest createRequest = mapper.toCreateRequest(item, sortedAnswers, query);
                    String newContentHash = HashUtil.calculateSha256(createRequest.content());

                    if (existingSync.isPresent()) {
                        StackOverflowSyncQuestion sync = existingSync.get();
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
                                    questionsImported++;

                                    registerSync(item, newContentHash, existingDocId);

                                    sync.setLastSyncedAt(LocalDateTime.now());
                                    sync.setTitle(item.title());
                                    repository.save(sync);
                                }
                            } catch (DocumentNotFoundException e) {
                                DocumentResponse created = documentService.create(createRequest);
                                questionsImported++;

                                registerSync(item, newContentHash, created.id());

                                sync.setDocumentId(created.id());
                                sync.setLastSyncedAt(LocalDateTime.now());
                                sync.setTitle(item.title());
                                repository.save(sync);
                            }
                        } else {
                            try {
                                DocumentResponse created = documentService.create(createRequest);
                                questionsImported++;

                                registerSync(item, newContentHash, created.id());

                                sync.setDocumentId(created.id());
                                sync.setLastSyncedAt(LocalDateTime.now());
                                sync.setTitle(item.title());
                                repository.save(sync);
                            } catch (DuplicateDocumentException e) {
                                duplicatesSkipped++;
                            }
                        }
                    } else {
                        try {
                             DocumentResponse created = documentService.create(createRequest);
                             questionsImported++;

                             registerSync(item, newContentHash, created.id());

                             StackOverflowSyncQuestion sync = new StackOverflowSyncQuestion(
                                     item.questionId(),
                                     item.title(),
                                     LocalDateTime.now(),
                                     created.id()
                             );
                             repository.save(sync);
                         } catch (DuplicateDocumentException e) {
                             duplicatesSkipped++;
                            StackOverflowSyncQuestion sync = new StackOverflowSyncQuestion(
                                    item.questionId(),
                                    item.title(),
                                    LocalDateTime.now(),
                                    null
                            );
                            repository.save(sync);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error importing Stack Overflow question [{}]: {}", item.questionId(), e.getMessage());
                }
            }

            if (!searchResponse.hasMore()) {
                hasMoreResults = false;
            } else {
                page++;
            }
        }

        return new StackOverflowDiscoverResponse(
                query,
                questionsDiscovered,
                questionsImported,
                duplicatesSkipped
        );
    }

    @Override
    public StackOverflowSyncStatusResponse getSyncStatus() {
        long totalQuestions = repository.count();
        List<StackOverflowSyncStatusResponse.QuestionSyncInfo> list = repository.findAll().stream()
                .map(q -> new StackOverflowSyncStatusResponse.QuestionSyncInfo(
                        q.getQuestionId(),
                        q.getTitle(),
                        q.getLastSyncedAt()
                ))
                .toList();
        return new StackOverflowSyncStatusResponse(totalQuestions, list);
    }
}
