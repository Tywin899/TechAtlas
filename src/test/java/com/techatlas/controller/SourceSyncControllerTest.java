package com.techatlas.controller;

import com.techatlas.dto.SourceSyncResponse;
import com.techatlas.dto.SourceSyncStatusResponse;
import com.techatlas.entity.SourceType;
import com.techatlas.service.SourceSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SourceSyncController.class)
@ActiveProfiles("test")
class SourceSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SourceSyncService service;

    @MockBean
    private com.techatlas.config.SyncSchedulerProperties schedulerProperties;

    @Test
    void testSyncSourceSuccess() throws Exception {
        SourceSyncResponse response = new SourceSyncResponse(
                SourceType.WIKIPEDIA, 5, 0, 1, 4, 0, 0, 0, 1, 1
        );

        when(service.syncSource(SourceType.WIKIPEDIA)).thenReturn(response);

        mockMvc.perform(post("/api/v1/sync/WIKIPEDIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("WIKIPEDIA"))
                .andExpect(jsonPath("$.checked").value(5))
                .andExpect(jsonPath("$.changedResources").value(1))
                .andExpect(jsonPath("$.unchangedResources").value(4));
    }

    @Test
    void testSyncSourceManualUnsupported() throws Exception {
        mockMvc.perform(post("/api/v1/sync/MANUAL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSyncSourceInvalidEnum() throws Exception {
        mockMvc.perform(post("/api/v1/sync/INVALID_SOURCE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSyncStatusSuccess() throws Exception {
        SourceSyncStatusResponse response = new SourceSyncStatusResponse(0L, List.of());

        when(service.getStatusSummary(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTracked").value(0L));
    }

    @Test
    void testGetSchedulerStatusSuccess() throws Exception {
        com.techatlas.config.SyncSchedulerProperties.SourceConfig wikiConfig = new com.techatlas.config.SyncSchedulerProperties.SourceConfig();
        wikiConfig.setEnabled(true);
        com.techatlas.config.SyncSchedulerProperties.SourceConfig gitConfig = new com.techatlas.config.SyncSchedulerProperties.SourceConfig();
        gitConfig.setEnabled(false);
        com.techatlas.config.SyncSchedulerProperties.SourceConfig soConfig = new com.techatlas.config.SyncSchedulerProperties.SourceConfig();
        soConfig.setEnabled(true);

        when(schedulerProperties.isEnabled()).thenReturn(true);
        when(schedulerProperties.getWikipedia()).thenReturn(wikiConfig);
        when(schedulerProperties.getGithub()).thenReturn(gitConfig);
        when(schedulerProperties.getStackoverflow()).thenReturn(soConfig);
        when(schedulerProperties.getFixedDelayMs()).thenReturn(3600000L);
        when(service.getRunningSources()).thenReturn(java.util.Set.of(SourceType.WIKIPEDIA));

        mockMvc.perform(get("/api/v1/sync/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.intervalMs").value(3600000))
                .andExpect(jsonPath("$.runningSources[0]").value("WIKIPEDIA"))
                .andExpect(jsonPath("$.configuredSources.WIKIPEDIA").value(true))
                .andExpect(jsonPath("$.configuredSources.GITHUB").value(false));
    }
}
