package com.techatlas.repository;

import com.techatlas.entity.SourceSyncRecord;
import com.techatlas.entity.SourceType;
import com.techatlas.entity.SyncStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class SourceSyncRecordRepositoryTest {

    @Autowired
    private SourceSyncRecordRepository repository;

    private SourceSyncRecord record;
    private UUID testDocId;

    @BeforeEach
    void setUp() {
        testDocId = UUID.randomUUID();
        record = new SourceSyncRecord(
                SourceType.WIKIPEDIA,
                "12345",
                testDocId,
                "rev1",
                "hash123",
                SyncStatus.SYNCED
        );
        record = repository.saveAndFlush(record);
    }

    @Test
    void testSaveAndFind() {
        assertNotNull(record.getId());
        Optional<SourceSyncRecord> found = repository.findById(record.getId());
        assertTrue(found.isPresent());
        assertEquals("12345", found.get().getExternalId());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void testFindBySourceAndExternalId() {
        Optional<SourceSyncRecord> found = repository.findBySourceAndExternalId(SourceType.WIKIPEDIA, "12345");
        assertTrue(found.isPresent());
        assertEquals(record.getId(), found.get().getId());
    }

    @Test
    void testFindBySource() {
        List<SourceSyncRecord> list = repository.findBySource(SourceType.WIKIPEDIA);
        assertEquals(1, list.size());

        List<SourceSyncRecord> githubList = repository.findBySource(SourceType.GITHUB);
        assertTrue(githubList.isEmpty());
    }

    @Test
    void testFindByStatus() {
        List<SourceSyncRecord> list = repository.findByStatus(SyncStatus.SYNCED);
        assertEquals(1, list.size());
    }

    @Test
    void testFindByDocumentId() {
        List<SourceSyncRecord> list = repository.findByDocumentId(testDocId);
        assertEquals(1, list.size());
    }

    @Test
    void testExistsBySourceAndExternalId() {
        assertTrue(repository.existsBySourceAndExternalId(SourceType.WIKIPEDIA, "12345"));
        assertFalse(repository.existsBySourceAndExternalId(SourceType.WIKIPEDIA, "999"));
    }

    @Test
    void testCountBySource() {
        assertEquals(1, repository.countBySource(SourceType.WIKIPEDIA));
        assertEquals(0, repository.countBySource(SourceType.GITHUB));
    }

    @Test
    void testUniqueConstraint() {
        SourceSyncRecord duplicate = new SourceSyncRecord(
                SourceType.WIKIPEDIA,
                "12345", // duplicate
                UUID.randomUUID(),
                "rev2",
                "hash456",
                SyncStatus.NEW
        );
        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.saveAndFlush(duplicate);
        });
    }
}
