package com.techatlas.controller;

import com.techatlas.autocomplete.service.AutocompleteService;
import com.techatlas.dto.AutocompleteResponse;
import com.techatlas.dto.AutocompleteStatusResponse;
import com.techatlas.dto.SuggestionItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AutocompleteController.class)
@ActiveProfiles("test")
public class AutocompleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AutocompleteService autocompleteService;

    @Test
    public void testGetSuggestionsSuccess() throws Exception {
        AutocompleteResponse mockResponse = new AutocompleteResponse(
                "spr",
                List.of(new SuggestionItem("spring", "TERM", 10)),
                1
        );

        when(autocompleteService.getSuggestions("spr", 5)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/search/suggestions")
                        .param("q", "spr")
                        .param("limit", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("spr"))
                .andExpect(jsonPath("$.suggestions[0].text").value("spring"))
                .andExpect(jsonPath("$.suggestions[0].type").value("TERM"))
                .andExpect(jsonPath("$.count").value(1));

        verify(autocompleteService, times(1)).getSuggestions("spr", 5);
    }

    @Test
    public void testGetStatusSuccess() throws Exception {
        AutocompleteStatusResponse mockStatus = new AutocompleteStatusResponse(
                true, 100, 100, 10, 5, 2
        );

        when(autocompleteService.getStatus()).thenReturn(mockStatus);

        mockMvc.perform(get("/api/v1/autocomplete/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.vocabularySize").value(100))
                .andExpect(jsonPath("$.prefixIndexTermCount").value(100))
                .andExpect(jsonPath("$.totalSuggestionsRequests").value(10))
                .andExpect(jsonPath("$.popularQueriesCount").value(5))
                .andExpect(jsonPath("$.recentQueriesCount").value(2));

        verify(autocompleteService, times(1)).getStatus();
    }
}
