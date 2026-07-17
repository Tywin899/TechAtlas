package com.techatlas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.exception.DocumentNotFoundException;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.service.DocumentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    private CreateDocumentRequest createRequest;
    private UpdateDocumentRequest updateRequest;
    private DocumentResponse documentResponse;
    private UUID docId;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        createRequest = new CreateDocumentRequest(
                "Doc Title",
                "Some unique content",
                "https://example.com/doc",
                SourceType.GITHUB,
                "DevOps",
                "Alice",
                "en",
                "{\"key\":\"value\"}"
        );

        updateRequest = new UpdateDocumentRequest(
                "Doc Title Updated",
                "Some unique content updated",
                "https://example.com/doc",
                SourceType.GITHUB,
                "DevOps",
                "Alice",
                "en",
                DocumentStatus.ACTIVE,
                "{\"key\":\"value\"}"
        );

        documentResponse = new DocumentResponse(
                docId,
                createRequest.title(),
                createRequest.content(),
                createRequest.url(),
                createRequest.source(),
                createRequest.category(),
                createRequest.author(),
                createRequest.language(),
                "hash123",
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                createRequest.metadata()
        );
    }

    @Test
    void testCreateSuccess() throws Exception {
        when(documentService.create(any(CreateDocumentRequest.class))).thenReturn(documentResponse);

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.title").value(createRequest.title()))
                .andExpect(jsonPath("$.url").value(createRequest.url()));
    }

    @Test
    void testCreateValidationFailure() throws Exception {
        CreateDocumentRequest invalidRequest = new CreateDocumentRequest(
                "", // Blank title
                "", // Blank content
                "not-a-valid-url", // Malformed URL
                null, // Null source
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Validation failed")));
    }

    @Test
    void testCreateDuplicateHashThrows409() throws Exception {
        when(documentService.create(any(CreateDocumentRequest.class)))
                .thenThrow(new DuplicateDocumentException("Document already exists"));

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Document already exists"));
    }

    @Test
    void testListAllSuccess() throws Exception {
        when(documentService.listAll()).thenReturn(List.of(documentResponse));

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(docId.toString()))
                .andExpect(jsonPath("$[0].title").value(createRequest.title()));
    }

    @Test
    void testRetrieveSuccess() throws Exception {
        when(documentService.retrieve(docId)).thenReturn(documentResponse);

        mockMvc.perform(get("/api/v1/documents/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.title").value(createRequest.title()));
    }

    @Test
    void testRetrieveNotFoundThrows404() throws Exception {
        when(documentService.retrieve(docId)).thenThrow(new DocumentNotFoundException("Document not found"));

        mockMvc.perform(get("/api/v1/documents/{id}", docId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found"));
    }

    @Test
    void testUpdateSuccess() throws Exception {
        DocumentResponse updatedResponse = new DocumentResponse(
                docId,
                updateRequest.title(),
                updateRequest.content(),
                updateRequest.url(),
                updateRequest.source(),
                updateRequest.category(),
                updateRequest.author(),
                updateRequest.language(),
                "hash123-updated",
                DocumentStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                updateRequest.metadata()
        );
        when(documentService.update(eq(docId), any(UpdateDocumentRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/documents/{id}", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updateRequest.title()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testUpdateNotFoundThrows404() throws Exception {
        when(documentService.update(eq(docId), any(UpdateDocumentRequest.class)))
                .thenThrow(new DocumentNotFoundException("Document not found"));

        mockMvc.perform(put("/api/v1/documents/{id}", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteSuccess() throws Exception {
        doNothing().when(documentService).delete(docId);

        mockMvc.perform(delete("/api/v1/documents/{id}", docId))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteNotFoundThrows404() throws Exception {
        doThrow(new DocumentNotFoundException("Document not found")).when(documentService).delete(docId);

        mockMvc.perform(delete("/api/v1/documents/{id}", docId))
                .andExpect(status().isNotFound());
    }
}
