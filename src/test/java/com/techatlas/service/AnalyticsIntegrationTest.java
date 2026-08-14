package com.techatlas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.index.IndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private IndexService indexService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.techatlas.repository.SearchAnalyticsRepository searchAnalyticsRepository;

    @BeforeEach
    public void setUp() {
        searchAnalyticsRepository.deleteAll();
    }

    @Test
    public void testE2EAnalyticsAndMonitoringFlow() throws Exception {
        // 1. Create/import a document
        CreateDocumentRequest docRequest = new CreateDocumentRequest(
                "E2E Integration Test Title",
                "This is the core content for testing analytics integrations with spring framework.",
                "http://e2e-integration.com",
                SourceType.MANUAL,
                "Java",
                "Author",
                "en",
                null
        );
        DocumentResponse doc = documentService.create(docRequest);
        assertThat(doc).isNotNull();

        // 2. Index document
        indexService.indexDocument(doc.id());

        // 3. Search existing query
        mockMvc.perform(get("/api/v1/search")
                .param("q", "analytics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 4. Search zero-result query
        mockMvc.perform(get("/api/v1/search")
                .param("q", "nonexistentterm")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 5. Search again to compile stats
        mockMvc.perform(get("/api/v1/search")
                .param("q", "analytics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 6. Query analytics search latency
        mockMvc.perform(get("/api/v1/analytics/search/latency"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"totalQueries\":3");
                });

        // 7. Query analytics top queries
        mockMvc.perform(get("/api/v1/analytics/search/top-queries").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"query\":\"analytics\",\"count\":2");
                    assertThat(body).contains("\"query\":\"nonexistentterm\",\"count\":1");
                });

        // 8. Query analytics zero results
        mockMvc.perform(get("/api/v1/analytics/search/zero-results"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"query\":\"nonexistentterm\",\"count\":1");
                });

        // 9. Query document stats
        mockMvc.perform(get("/api/v1/analytics/documents"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"totalDocuments\"");
                    assertThat(body).contains("\"bySource\"");
                    assertThat(body).contains("\"byStatus\"");
                });

        // 10. Query index stats
        mockMvc.perform(get("/api/v1/analytics/index"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"vocabularySize\"");
                    assertThat(body).contains("\"totalPostings\"");
                });

        // 11. Query overview dashboard
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("\"search\"");
                    assertThat(body).contains("\"documents\"");
                    assertThat(body).contains("\"index\"");
                    assertThat(body).contains("\"cache\"");
                    assertThat(body).contains("\"synchronization\"");
                });
    }
}
