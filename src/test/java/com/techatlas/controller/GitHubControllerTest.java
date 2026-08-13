package com.techatlas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techatlas.dto.GitHubDiscoverRequest;
import com.techatlas.dto.GitHubDiscoverResponse;
import com.techatlas.dto.GitHubSyncStatusResponse;
import com.techatlas.service.GitHubService;
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

@WebMvcTest(GitHubController.class)
@ActiveProfiles("test")
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitHubService githubService;

    @Test
    void testDiscoverSuccess() throws Exception {
        GitHubDiscoverRequest request = new GitHubDiscoverRequest("spring-boot", 10);
        GitHubDiscoverResponse response = new GitHubDiscoverResponse("spring-boot", 15, 10, 5);

        when(githubService.discoverRepositories(any(GitHubDiscoverRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/github/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("spring-boot"))
                .andExpect(jsonPath("$.repositoriesDiscovered").value(15))
                .andExpect(jsonPath("$.repositoriesImported").value(10))
                .andExpect(jsonPath("$.duplicatesSkipped").value(5));
    }

    @Test
    void testDiscoverValidationFailure() throws Exception {
        GitHubDiscoverRequest request = new GitHubDiscoverRequest("", -1);

        mockMvc.perform(post("/api/v1/github/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSyncStatusSuccess() throws Exception {
        GitHubSyncStatusResponse response = new GitHubSyncStatusResponse(5L, List.of());

        when(githubService.getSyncStatus()).thenReturn(response);

        mockMvc.perform(get("/api/v1/github/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSyncedRepositories").value(5L));
    }
}
