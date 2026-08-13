package com.techatlas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.exception.WikipediaPageNotFoundException;
import com.techatlas.exception.WikipediaUnavailableException;
import com.techatlas.dto.WikipediaDiscoverRequest;
import com.techatlas.dto.WikipediaDiscoverResponse;
import com.techatlas.dto.WikipediaSyncStatusResponse;
import com.techatlas.fetcher.wikipedia.dto.WikipediaImportRequest;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.service.WikipediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WikipediaController.class)
@ActiveProfiles("test")
class WikipediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WikipediaService wikipediaService;

    private WikipediaImportRequest importRequest;
    private WikipediaPageSummary summaryResponse;
    private DocumentResponse documentResponse;

    @BeforeEach
    void setUp() {
        importRequest = new WikipediaImportRequest("Java");
        summaryResponse = new WikipediaPageSummary(
                "Java",
                "Java is high-level.",
                "OOP Language",
                12345L,
                "en",
                "123",
                null,
                null
        );
        documentResponse = new DocumentResponse(
                UUID.randomUUID(),
                "Java",
                "Java is high-level.",
                "https://en.wikipedia.org/wiki/Java",
                SourceType.WIKIPEDIA,
                null,
                "Wikipedia",
                "en",
                "hash123",
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "{}"
        );
    }

    @Test
    void testImportArticleSuccess() throws Exception {
        when(wikipediaService.importArticle("Java")).thenReturn(documentResponse);

        mockMvc.perform(post("/api/v1/wikipedia/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java"))
                .andExpect(jsonPath("$.source").value("WIKIPEDIA"));
    }

    @Test
    void testImportArticleValidationFailure() throws Exception {
        WikipediaImportRequest invalid = new WikipediaImportRequest("");

        mockMvc.perform(post("/api/v1/wikipedia/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testImportArticleNotFound() throws Exception {
        when(wikipediaService.importArticle("Java"))
                .thenThrow(new WikipediaPageNotFoundException("Page not found"));

        mockMvc.perform(post("/api/v1/wikipedia/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Page not found"));
    }

    @Test
    void testImportArticleDuplicateThrows409() throws Exception {
        when(wikipediaService.importArticle("Java"))
                .thenThrow(new DuplicateDocumentException("Article already exists"));

        mockMvc.perform(post("/api/v1/wikipedia/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Article already exists"));
    }

    @Test
    void testImportArticleUnavailableThrows503() throws Exception {
        when(wikipediaService.importArticle("Java"))
                .thenThrow(new WikipediaUnavailableException("Service unavailable"));

        mockMvc.perform(post("/api/v1/wikipedia/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void testGetSummarySuccess() throws Exception {
        when(wikipediaService.fetchSummary("Java")).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/v1/wikipedia/{title}", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java"))
                .andExpect(jsonPath("$.extract").value("Java is high-level."));
    }

    @Test
    void testDiscoverSuccess() throws Exception {
        WikipediaDiscoverRequest request = new WikipediaDiscoverRequest("Java_programming", 10, 1);
        WikipediaDiscoverResponse response = new WikipediaDiscoverResponse("Java_programming", 15, 10, 5, 2);

        when(wikipediaService.discoverArticles(any(WikipediaDiscoverRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/wikipedia/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Java_programming"))
                .andExpect(jsonPath("$.articlesDiscovered").value(15))
                .andExpect(jsonPath("$.articlesImported").value(10))
                .andExpect(jsonPath("$.duplicatesSkipped").value(5))
                .andExpect(jsonPath("$.categoriesVisited").value(2));
    }

    @Test
    void testDiscoverValidationFailure() throws Exception {
        WikipediaDiscoverRequest request = new WikipediaDiscoverRequest("", -1, 10);

        mockMvc.perform(post("/api/v1/wikipedia/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSyncStatusSuccess() throws Exception {
        WikipediaSyncStatusResponse response = new WikipediaSyncStatusResponse(5L, 20L, List.of());

        when(wikipediaService.getSyncStatus()).thenReturn(response);

        mockMvc.perform(get("/api/v1/wikipedia/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSyncedCategories").value(5L))
                .andExpect(jsonPath("$.totalSyncedArticles").value(20L));
    }
}
