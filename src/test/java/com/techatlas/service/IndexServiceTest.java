package com.techatlas.service;

import com.techatlas.config.IndexProperties;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import com.techatlas.exception.TechAtlasException;
import com.techatlas.index.IndexService;
import com.techatlas.index.IndexServiceImpl;
import com.techatlas.model.InvertedIndex;
import com.techatlas.model.Posting;
import com.techatlas.normalizer.TextNormalizer;
import com.techatlas.stemmer.PorterStemmerAdapter;
import com.techatlas.stopwords.StopWordFilter;
import com.techatlas.tokenizer.Tokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IndexServiceTest {

    @Mock
    private DocumentService documentService;

    private Tokenizer tokenizer;
    private TextNormalizer textNormalizer;
    private StopWordFilter stopWordFilter;
    private PorterStemmerAdapter porterStemmerAdapter;
    private InvertedIndex invertedIndex;
    @Mock
    private com.techatlas.cache.CacheService cacheService;

    private IndexService indexService;

    @BeforeEach
    public void setUp() {
        IndexProperties properties = new IndexProperties();
        properties.setStopwords(List.of("the", "is", "a", "and"));
        properties.setStemming(true);
        properties.setNormalization(true);

        tokenizer = new Tokenizer();
        textNormalizer = new TextNormalizer(properties);
        stopWordFilter = new StopWordFilter(properties);
        porterStemmerAdapter = new PorterStemmerAdapter(properties);
        invertedIndex = new InvertedIndex();

        indexService = new IndexServiceImpl(
                documentService,
                tokenizer,
                textNormalizer,
                stopWordFilter,
                porterStemmerAdapter,
                invertedIndex,
                cacheService
        );
    }

    @Test
    public void testIndexDocumentSuccess() {
        UUID docId = UUID.randomUUID();
        DocumentResponse doc = new DocumentResponse(
                docId,
                "Title",
                "Spring Boot simplifies Java development.",
                "http://url.com",
                SourceType.MANUAL,
                "tech",
                "author",
                "en",
                "hash",
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );

        when(documentService.retrieve(docId)).thenReturn(doc);

        indexService.indexDocument(docId);

        assertThat(invertedIndex.retrieve("spring")).isNotNull();
        assertThat(invertedIndex.retrieve("boot")).isNotNull();
        assertThat(invertedIndex.retrieve("java")).isNotNull();
        assertThat(invertedIndex.retrieve("develop")).isNotNull();

        verify(documentService, times(1)).updateStatus(eq(docId), eq(DocumentStatus.ACTIVE), any());
    }

    @Test
    public void testIndexDocumentEmptyContentSkipsSafely() {
        UUID docId = UUID.randomUUID();
        DocumentResponse doc = new DocumentResponse(
                docId,
                "Title",
                "   ",
                "http://url.com",
                SourceType.MANUAL,
                "tech",
                "author",
                "en",
                "hash",
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );

        when(documentService.retrieve(docId)).thenReturn(doc);

        indexService.indexDocument(docId);

        assertThat(invertedIndex.getDocumentCount()).isEqualTo(0);
        verify(documentService, times(1)).updateStatus(eq(docId), eq(DocumentStatus.ACTIVE), any());
    }

    @Test
    public void testIndexDocumentFailsUpdatesStatus() {
        UUID docId = UUID.randomUUID();
        DocumentResponse doc = new DocumentResponse(
                docId,
                "Title",
                "Spring Boot java",
                "http://url.com",
                SourceType.MANUAL,
                "tech",
                "author",
                "en",
                "hash",
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );

        when(documentService.retrieve(docId)).thenReturn(doc);
        when(documentService.updateStatus(eq(docId), eq(DocumentStatus.ACTIVE), any())).thenThrow(new RuntimeException("DB Error"));

        assertThrows(TechAtlasException.class, () -> indexService.indexDocument(docId));

        verify(documentService, times(1)).updateStatus(eq(docId), eq(DocumentStatus.FAILED), eq(null));
    }

    @Test
    public void testRebuildIndex() {
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();

        DocumentResponse doc1 = new DocumentResponse(docA, "A", "java spring", "url", SourceType.MANUAL, "cat", "auth", "en", "h1", DocumentStatus.PENDING_INDEX, LocalDateTime.now(), LocalDateTime.now(), null, null);
        DocumentResponse doc2 = new DocumentResponse(docB, "B", "java boot", "url", SourceType.MANUAL, "cat", "auth", "en", "h2", DocumentStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null);

        when(documentService.listAll()).thenReturn(List.of(doc1, doc2));
        when(documentService.retrieve(docA)).thenReturn(doc1);
        when(documentService.retrieve(docB)).thenReturn(doc2);

        indexService.rebuildIndex();

        assertThat(invertedIndex.getDocumentCount()).isEqualTo(2);
        assertThat(invertedIndex.retrieve("java").getPostings()).hasSize(2);
        assertThat(invertedIndex.retrieve("spring").getPostings()).hasSize(1);
        assertThat(invertedIndex.retrieve("boot").getPostings()).hasSize(1);
    }

    @Test
    public void testRemoveDocument() {
        UUID docId = UUID.randomUUID();
        invertedIndex.insert("java", new Posting(docId, 1));
        assertThat(invertedIndex.getDocumentCount()).isEqualTo(1);

        indexService.removeDocument(docId);

        assertThat(invertedIndex.getDocumentCount()).isEqualTo(0);
    }

    @Test
    public void testReindexDocument() {
        UUID docId = UUID.randomUUID();
        DocumentResponse doc = new DocumentResponse(
                docId,
                "Title",
                "New Content",
                "http://url.com",
                SourceType.MANUAL,
                "tech",
                "author",
                "en",
                "hash",
                DocumentStatus.PENDING_INDEX,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null
        );

        when(documentService.retrieve(docId)).thenReturn(doc);

        indexService.reindexDocument(docId);

        assertThat(invertedIndex.retrieve("new")).isNotNull();
        assertThat(invertedIndex.retrieve("content")).isNotNull();
        verify(documentService, times(1)).updateStatus(eq(docId), eq(DocumentStatus.ACTIVE), any());
    }

    @Test
    public void testIndexPendingDocuments() {
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();
        UUID docC = UUID.randomUUID();

        DocumentResponse doc1 = new DocumentResponse(docA, "A", "java spring", "url", SourceType.MANUAL, "cat", "auth", "en", "h1", DocumentStatus.PENDING_INDEX, LocalDateTime.now(), LocalDateTime.now(), null, null);
        DocumentResponse doc2 = new DocumentResponse(docB, "B", "java boot", "url", SourceType.MANUAL, "cat", "auth", "en", "h2", DocumentStatus.PENDING_INDEX, LocalDateTime.now(), LocalDateTime.now(), null, null);
        DocumentResponse doc3 = new DocumentResponse(docC, "C", "java clean", "url", SourceType.MANUAL, "cat", "auth", "en", "h3", DocumentStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null);

        when(documentService.listAll()).thenReturn(List.of(doc1, doc2, doc3));
        when(documentService.retrieve(docA)).thenReturn(doc1);
        when(documentService.retrieve(docB)).thenReturn(doc2);

        indexService.indexPendingDocuments();

        assertThat(invertedIndex.getDocumentCount()).isEqualTo(2);
        assertThat(invertedIndex.retrieve("spring")).isNotNull();
        assertThat(invertedIndex.retrieve("boot")).isNotNull();
        assertThat(invertedIndex.retrieve("clean")).isNull(); // was already ACTIVE, skipped by pending indexer
        verify(documentService, times(2)).updateStatus(any(), eq(DocumentStatus.ACTIVE), any());
    }
}
