package com.techatlas.controller;

import com.techatlas.exception.DocumentNotFoundException;
import com.techatlas.exception.TechAtlasException;
import com.techatlas.index.IndexService;
import com.techatlas.model.InvertedIndex;
import com.techatlas.model.Posting;
import com.techatlas.model.PostingList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndexController.class)
@ActiveProfiles("test")
public class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IndexService indexService;

    @MockBean
    private InvertedIndex invertedIndex;

    @Test
    public void testRebuildSuccess() throws Exception {
        doNothing().when(indexService).rebuildIndex();

        mockMvc.perform(post("/api/v1/index/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Index rebuilt successfully"));

        verify(indexService, times(1)).rebuildIndex();
    }

    @Test
    public void testIndexDocumentSuccess() throws Exception {
        UUID docId = UUID.randomUUID();
        doNothing().when(indexService).indexDocument(docId);

        mockMvc.perform(post("/api/v1/index/document/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document indexed successfully"));

        verify(indexService, times(1)).indexDocument(docId);
    }

    @Test
    public void testIndexDocumentNotFound() throws Exception {
        UUID docId = UUID.randomUUID();
        doThrow(new DocumentNotFoundException("Document not found with ID: " + docId))
                .when(indexService).indexDocument(docId);

        mockMvc.perform(post("/api/v1/index/document/{id}", docId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found with ID: " + docId));

        verify(indexService, times(1)).indexDocument(docId);
    }

    @Test
    public void testIndexDocumentFailure() throws Exception {
        UUID docId = UUID.randomUUID();
        doThrow(new TechAtlasException(HttpStatus.INTERNAL_SERVER_ERROR, "Indexing failed"))
                .when(indexService).indexDocument(docId);

        mockMvc.perform(post("/api/v1/index/document/{id}", docId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Indexing failed"));

        verify(indexService, times(1)).indexDocument(docId);
    }

    @Test
    public void testGetStatusSuccess() throws Exception {
        when(invertedIndex.getDocumentCount()).thenReturn(2);
        when(invertedIndex.getVocabularySize()).thenReturn(5);

        PostingList pList = new PostingList(List.of(new Posting(UUID.randomUUID(), 2)));
        Map<String, PostingList> mockMap = Map.of("term", pList);
        when(invertedIndex.getIndex()).thenReturn(mockMap);

        mockMvc.perform(get("/api/v1/index/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexedDocuments").value(2))
                .andExpect(jsonPath("$.vocabularySize").value(5))
                .andExpect(jsonPath("$.uniqueTerms").value(5))
                .andExpect(jsonPath("$.totalPostings").value(1));
    }
}
