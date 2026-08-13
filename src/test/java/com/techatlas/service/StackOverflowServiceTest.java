package com.techatlas.service;

import com.techatlas.config.StackOverflowProperties;
import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.StackOverflowDiscoverRequest;
import com.techatlas.dto.StackOverflowDiscoverResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.entity.StackOverflowSyncQuestion;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.fetcher.stackoverflow.StackOverflowClient;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowOwner;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowResponseWrapper;
import com.techatlas.mapper.StackOverflowMapper;
import com.techatlas.repository.StackOverflowSyncQuestionRepository;
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
class StackOverflowServiceTest {

    @Mock
    private StackOverflowClient client;

    @Mock
    private DocumentService documentService;

    @Mock
    private StackOverflowSyncQuestionRepository repository;

    @Mock
    private SourceSyncService sourceSyncService;

    private StackOverflowMapper mapper;
    private StackOverflowProperties properties;
    private StackOverflowService service;

    private StackOverflowQuestionItem questionItem;
    private StackOverflowAnswerItem answerItem;
    private DocumentResponse documentResponse;

    @BeforeEach
    void setUp() {
        mapper = new StackOverflowMapper(new ObjectMapper());
        properties = new StackOverflowProperties();
        properties.setDefaultPageSize(10);
        properties.setMaxAnswersPerQuestion(1);

        service = new StackOverflowServiceImpl(client, mapper, documentService, repository, properties, sourceSyncService);

        questionItem = new StackOverflowQuestionItem(
                112233L,
                "Spring Boot",
                "<p>Question Body</p>",
                "https://stackoverflow.com/questions/112233",
                10,
                List.of("spring-boot"),
                new StackOverflowOwner("John Doe", 99L),
                true,
                1,
                5566L,
                1628859600L
        );

        answerItem = new StackOverflowAnswerItem(
                5566L,
                "<p>Answer Body</p>",
                20,
                true,
                new StackOverflowOwner("Jane Smith", 88L)
        );

        CreateDocumentRequest createRequest = mapper.toCreateRequest(questionItem, List.of(answerItem), "spring-boot");

        documentResponse = new DocumentResponse(
                UUID.randomUUID(),
                questionItem.title(),
                createRequest.content(),
                questionItem.link(),
                SourceType.STACKOVERFLOW,
                "spring-boot",
                "John Doe",
                "en",
                HashUtil.calculateSha256(createRequest.content()),
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "{}"
        );
    }

    @Test
    void testDiscoverQuestionsSuccess() {
        StackOverflowDiscoverRequest request = new StackOverflowDiscoverRequest("spring-boot", null, 1);

        when(client.searchQuestions(eq("spring-boot"), any(), eq(1), anyInt()))
                .thenReturn(new StackOverflowResponseWrapper<>(List.of(questionItem), false, 100, 100));
        when(client.fetchAnswers(112233L))
                .thenReturn(new StackOverflowResponseWrapper<>(List.of(answerItem), false, 100, 100));
        when(repository.findByQuestionId(112233L)).thenReturn(Optional.empty());
        when(documentService.create(any(CreateDocumentRequest.class))).thenReturn(documentResponse);

        StackOverflowDiscoverResponse response = service.discoverQuestions(request);

        assertNotNull(response);
        assertEquals("spring-boot", response.query());
        assertEquals(1, response.questionsDiscovered());
        assertEquals(1, response.questionsImported());
        assertEquals(0, response.duplicatesSkipped());

        verify(repository, times(1)).save(any(StackOverflowSyncQuestion.class));
    }

    @Test
    void testDiscoverQuestionsDuplicateSkipped() {
        StackOverflowDiscoverRequest request = new StackOverflowDiscoverRequest("spring-boot", null, 1);

        when(client.searchQuestions(eq("spring-boot"), any(), eq(1), anyInt()))
                .thenReturn(new StackOverflowResponseWrapper<>(List.of(questionItem), false, 100, 100));
        when(client.fetchAnswers(112233L))
                .thenReturn(new StackOverflowResponseWrapper<>(List.of(answerItem), false, 100, 100));

        StackOverflowSyncQuestion syncRecord = new StackOverflowSyncQuestion(
                112233L, "Spring Boot", LocalDateTime.now(), documentResponse.id()
        );
        when(repository.findByQuestionId(112233L)).thenReturn(Optional.of(syncRecord));
        when(documentService.retrieve(documentResponse.id())).thenReturn(documentResponse);

        StackOverflowDiscoverResponse response = service.discoverQuestions(request);

        assertNotNull(response);
        assertEquals(0, response.questionsImported());
        assertEquals(1, response.duplicatesSkipped());
        verify(documentService, never()).create(any());
        verify(documentService, never()).update(any(), any());
    }

    @Test
    void testDiscoverQuestionsUpdatedOnContentChange() {
        StackOverflowDiscoverRequest request = new StackOverflowDiscoverRequest("spring-boot", null, 1);

        when(client.searchQuestions(eq("spring-boot"), any(), eq(1), anyInt()))
                .thenReturn(new StackOverflowResponseWrapper<>(List.of(questionItem), false, 100, 100));

        StackOverflowAnswerItem differentAnswer = new StackOverflowAnswerItem(
                5566L, "<p>Different Answer Body</p>", 20, true, new StackOverflowOwner("Jane Smith", 88L)
        );
        when(client.fetchAnswers(112233L))
                .thenReturn(new StackOverflowResponseWrapper<>(List.of(differentAnswer), false, 100, 100));

        StackOverflowSyncQuestion syncRecord = new StackOverflowSyncQuestion(
                112233L, "Spring Boot", LocalDateTime.now(), documentResponse.id()
        );
        when(repository.findByQuestionId(112233L)).thenReturn(Optional.of(syncRecord));
        when(documentService.retrieve(documentResponse.id())).thenReturn(documentResponse);

        StackOverflowDiscoverResponse response = service.discoverQuestions(request);

        assertNotNull(response);
        assertEquals(1, response.questionsImported());
        assertEquals(0, response.duplicatesSkipped());

        verify(documentService, times(1)).update(eq(documentResponse.id()), any(UpdateDocumentRequest.class));
    }
}
