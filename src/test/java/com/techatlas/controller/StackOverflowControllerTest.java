package com.techatlas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techatlas.dto.StackOverflowDiscoverRequest;
import com.techatlas.dto.StackOverflowDiscoverResponse;
import com.techatlas.dto.StackOverflowSyncStatusResponse;
import com.techatlas.service.StackOverflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StackOverflowController.class)
@ActiveProfiles("test")
class StackOverflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StackOverflowService service;

    @Test
    void testDiscoverSuccess() throws Exception {
        StackOverflowDiscoverRequest request = new StackOverflowDiscoverRequest("spring-boot", List.of("spring-boot"), 10);
        StackOverflowDiscoverResponse response = new StackOverflowDiscoverResponse("spring-boot", 15, 10, 5);

        when(service.discoverQuestions(any(StackOverflowDiscoverRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/stackoverflow/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("spring-boot"))
                .andExpect(jsonPath("$.questionsDiscovered").value(15))
                .andExpect(jsonPath("$.questionsImported").value(10))
                .andExpect(jsonPath("$.duplicatesSkipped").value(5));
    }

    @Test
    void testDiscoverValidationFailure() throws Exception {
        StackOverflowDiscoverRequest request = new StackOverflowDiscoverRequest("", null, -1);

        mockMvc.perform(post("/api/v1/stackoverflow/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSyncStatusSuccess() throws Exception {
        StackOverflowSyncStatusResponse response = new StackOverflowSyncStatusResponse(5L, List.of());

        when(service.getSyncStatus()).thenReturn(response);

        mockMvc.perform(get("/api/v1/stackoverflow/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSyncedQuestions").value(5L));
    }
}
