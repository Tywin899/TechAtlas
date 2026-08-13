package com.techatlas.controller;

import com.techatlas.dto.SearchRequest;
import com.techatlas.dto.SearchResponse;
import com.techatlas.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@ActiveProfiles("test")
public class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    public void testSearchEndpointSuccess() throws Exception {
        SearchResponse mockResponse = new SearchResponse("spring", 0, Collections.emptyList(), 0, 10, 0);
        when(searchService.search(any(SearchRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/search").param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("spring"))
                .andExpect(jsonPath("$.totalResults").value(0));

        verify(searchService, times(1)).search(any(SearchRequest.class));
    }

    @Test
    public void testSearchEndpointValidationFailure() throws Exception {
        when(searchService.search(any(SearchRequest.class)))
                .thenThrow(new IllegalArgumentException("Query parameter 'q' must not be empty"));

        mockMvc.perform(get("/api/v1/search").param("q", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Query parameter 'q' must not be empty"));
    }
}
