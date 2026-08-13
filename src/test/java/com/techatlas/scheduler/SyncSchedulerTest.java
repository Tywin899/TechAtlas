package com.techatlas.scheduler;

import com.techatlas.config.SyncSchedulerProperties;
import com.techatlas.entity.SourceType;
import com.techatlas.service.SourceSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncSchedulerTest {

    @Mock
    private SourceSyncService sourceSyncService;

    private SyncSchedulerProperties properties;
    private SyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new SyncSchedulerProperties();
        scheduler = new SyncScheduler(sourceSyncService, properties);
    }

    @Test
    void testSchedulerRunsWhenEnabled() {
        properties.setEnabled(true);
        properties.getWikipedia().setEnabled(true);
        properties.getGithub().setEnabled(true);
        properties.getStackoverflow().setEnabled(true);

        scheduler.runScheduledSync();

        verify(sourceSyncService, times(1)).syncSource(SourceType.WIKIPEDIA);
        verify(sourceSyncService, times(1)).syncSource(SourceType.GITHUB);
        verify(sourceSyncService, times(1)).syncSource(SourceType.STACKOVERFLOW);
    }

    @Test
    void testSchedulerDoesNothingWhenDisabled() {
        properties.setEnabled(false);

        scheduler.runScheduledSync();

        verify(sourceSyncService, never()).syncSource(any());
    }

    @Test
    void testSchedulerSkipsDisabledSources() {
        properties.setEnabled(true);
        properties.getWikipedia().setEnabled(true);
        properties.getGithub().setEnabled(false);
        properties.getStackoverflow().setEnabled(true);

        scheduler.runScheduledSync();

        verify(sourceSyncService, times(1)).syncSource(SourceType.WIKIPEDIA);
        verify(sourceSyncService, never()).syncSource(SourceType.GITHUB);
        verify(sourceSyncService, times(1)).syncSource(SourceType.STACKOVERFLOW);
    }

    @Test
    void testFailureIsolation() {
        properties.setEnabled(true);
        properties.getWikipedia().setEnabled(true);
        properties.getGithub().setEnabled(true);
        properties.getStackoverflow().setEnabled(true);

        when(sourceSyncService.syncSource(SourceType.WIKIPEDIA)).thenThrow(new RuntimeException("API failure"));

        scheduler.runScheduledSync();

        verify(sourceSyncService, times(1)).syncSource(SourceType.WIKIPEDIA);
        verify(sourceSyncService, times(1)).syncSource(SourceType.GITHUB);
        verify(sourceSyncService, times(1)).syncSource(SourceType.STACKOVERFLOW);
    }

    @Test
    void testOverlappingSkipsInvocation() {
        properties.setEnabled(true);
        properties.getWikipedia().setEnabled(true);
        properties.getGithub().setEnabled(false);
        properties.getStackoverflow().setEnabled(false);

        when(sourceSyncService.syncSource(SourceType.WIKIPEDIA)).thenThrow(new IllegalStateException("Already running"));

        scheduler.runScheduledSync();

        verify(sourceSyncService, times(1)).syncSource(SourceType.WIKIPEDIA);
    }
}
