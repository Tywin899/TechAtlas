package com.techatlas.service;

import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.SourceSyncResponse;
import com.techatlas.dto.SourceSyncStatusResponse;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceSyncRecord;
import com.techatlas.entity.SourceType;
import com.techatlas.entity.SyncStatus;
import com.techatlas.index.IndexService;
import com.techatlas.repository.SourceSyncRecordRepository;
import com.techatlas.sync.SourceResource;
import com.techatlas.sync.SourceSynchronizer;
import com.techatlas.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceSyncServiceTest {

    @Mock
    private SourceSyncRecordRepository repository;

    @Mock
    private DocumentService documentService;

    @Mock
    private IndexService indexService;

    @Mock
    private SourceSynchronizer synchronizer;

    private SourceSyncService service;

    private UUID documentId;
    private DocumentResponse documentResponse;
    private SourceSyncRecord syncRecord;

    @BeforeEach
    void setUp() {
        when(synchronizer.getSource()).thenReturn(SourceType.WIKIPEDIA);
        service = new SourceSyncServiceImpl(repository, documentService, indexService, List.of(synchronizer));

        documentId = UUID.randomUUID();
        documentResponse = new DocumentResponse(
                documentId,
                "Adoptium",
                "Clean extracted adoptium text",
                "https://en.wikipedia.org/wiki/Adoptium",
                SourceType.WIKIPEDIA,
                "Java",
                "Wikipedia",
                "en",
                HashUtil.calculateSha256("Clean extracted adoptium text"),
                DocumentStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "{}"
        );

        syncRecord = new SourceSyncRecord(
                SourceType.WIKIPEDIA,
                "554433",
                documentId,
                "rev1",
                HashUtil.calculateSha256("Clean extracted adoptium text"),
                SyncStatus.SYNCED
        );
    }

    @Test
    void testSyncSourceUnchanged() throws Exception {
        when(repository.findBySource(SourceType.WIKIPEDIA)).thenReturn(List.of(syncRecord));
        when(documentService.retrieve(documentId)).thenReturn(documentResponse);

        SourceResource resource = new SourceResource(
                SourceType.WIKIPEDIA,
                "554433",
                "rev1",
                "Adoptium",
                "Clean extracted adoptium text",
                "https://en.wikipedia.org/wiki/Adoptium",
                "Wikipedia",
                "en",
                "Java",
                "{}"
        );
        when(synchronizer.fetchResource("554433", "Adoptium")).thenReturn(resource);

        SourceSyncResponse response = service.syncSource(SourceType.WIKIPEDIA);

        assertNotNull(response);
        assertEquals(1, response.checked());
        assertEquals(1, response.unchangedResources());
        assertEquals(0, response.changedResources());
        assertEquals(0, response.failedResources());

        verify(documentService, never()).update(any(), any());
        verify(indexService, never()).indexDocument(any());
        assertEquals(SyncStatus.SYNCED, syncRecord.getStatus());
    }

    @Test
    void testSyncSourceChangedRevision() throws Exception {
        when(repository.findBySource(SourceType.WIKIPEDIA)).thenReturn(List.of(syncRecord));
        when(documentService.retrieve(documentId)).thenReturn(documentResponse);

        SourceResource resource = new SourceResource(
                SourceType.WIKIPEDIA,
                "554433",
                "rev2",
                "Adoptium",
                "Adoptium changed content text",
                "https://en.wikipedia.org/wiki/Adoptium",
                "Wikipedia",
                "en",
                "Java",
                "{}"
        );
        when(synchronizer.fetchResource("554433", "Adoptium")).thenReturn(resource);

        SourceSyncResponse response = service.syncSource(SourceType.WIKIPEDIA);

        assertNotNull(response);
        assertEquals(1, response.checked());
        assertEquals(1, response.changedResources());
        assertEquals(0, response.unchangedResources());
        assertEquals(1, response.updatedDocuments());

        verify(documentService, times(1)).update(eq(documentId), any());
        verify(indexService, times(1)).indexDocument(eq(documentId));
        assertEquals(SyncStatus.CHANGED, syncRecord.getStatus());
        assertEquals("rev2", syncRecord.getExternalRevision());
        assertEquals(HashUtil.calculateSha256("Adoptium changed content text"), syncRecord.getContentHash());
    }

    @Test
    void testSyncSourceFailure() throws Exception {
        when(repository.findBySource(SourceType.WIKIPEDIA)).thenReturn(List.of(syncRecord));
        when(documentService.retrieve(documentId)).thenReturn(documentResponse);
        when(synchronizer.fetchResource("554433", "Adoptium")).thenThrow(new RuntimeException("API connection timeout"));

        SourceSyncResponse response = service.syncSource(SourceType.WIKIPEDIA);

        assertNotNull(response);
        assertEquals(1, response.checked());
        assertEquals(1, response.failedResources());
        assertEquals(0, response.changedResources());

        assertEquals(SyncStatus.FAILED, syncRecord.getStatus());
        assertEquals("API connection timeout", syncRecord.getLastError());
    }
}
