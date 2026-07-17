package com.techatlas.repository;

import com.techatlas.entity.Document;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    private Document testDoc;

    @BeforeEach
    void setUp() {
        testDoc = new Document();
        testDoc.setTitle("Test Doc");
        testDoc.setContent("This is test content.");
        testDoc.setUrl("https://example.com/test");
        testDoc.setSource(SourceType.MANUAL);
        testDoc.setCategory("Programming");
        testDoc.setAuthor("John Doe");
        testDoc.setLanguage("en");
        testDoc.setContentHash("hash123");
        testDoc.setStatus(DocumentStatus.ACTIVE);
        testDoc = documentRepository.saveAndFlush(testDoc);
    }

    @Test
    void testSaveAndFindById() {
        assertNotNull(testDoc.getId());
        Optional<Document> found = documentRepository.findById(testDoc.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Doc", found.get().getTitle());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void testFindByStatus() {
        List<Document> activeDocs = documentRepository.findByStatus(DocumentStatus.ACTIVE);
        assertEquals(1, activeDocs.size());
        assertEquals(testDoc.getId(), activeDocs.get(0).getId());

        List<Document> pendingDocs = documentRepository.findByStatus(DocumentStatus.PENDING_INDEX);
        assertTrue(pendingDocs.isEmpty());
    }

    @Test
    void testFindBySource() {
        List<Document> manualDocs = documentRepository.findBySource(SourceType.MANUAL);
        assertEquals(1, manualDocs.size());

        List<Document> wikiDocs = documentRepository.findBySource(SourceType.WIKIPEDIA);
        assertTrue(wikiDocs.isEmpty());
    }

    @Test
    void testFindByContentHash() {
        Optional<Document> found = documentRepository.findByContentHash("hash123");
        assertTrue(found.isPresent());

        Optional<Document> notFound = documentRepository.findByContentHash("nonexistent");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testExistsByContentHash() {
        assertTrue(documentRepository.existsByContentHash("hash123"));
        assertFalse(documentRepository.existsByContentHash("nonexistent"));
    }

    @Test
    void testFindByCategory() {
        List<Document> categoryDocs = documentRepository.findByCategory("Programming");
        assertEquals(1, categoryDocs.size());

        List<Document> notFound = documentRepository.findByCategory("Cooking");
        assertTrue(notFound.isEmpty());
    }

    @Test
    void testFindByLanguage() {
        List<Document> langDocs = documentRepository.findByLanguage("en");
        assertEquals(1, langDocs.size());

        List<Document> notFound = documentRepository.findByLanguage("fr");
        assertTrue(notFound.isEmpty());
    }
}
