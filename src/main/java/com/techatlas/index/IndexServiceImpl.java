package com.techatlas.index;

import com.techatlas.dto.DocumentResponse;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.exception.TechAtlasException;
import com.techatlas.model.InvertedIndex;
import com.techatlas.model.Posting;
import com.techatlas.normalizer.TextNormalizer;
import com.techatlas.service.DocumentService;
import com.techatlas.stemmer.PorterStemmerAdapter;
import com.techatlas.stopwords.StopWordFilter;
import com.techatlas.tokenizer.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.techatlas.cache.CacheService;

@Service
public class IndexServiceImpl implements IndexService {

    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);

    private final DocumentService documentService;
    private final Tokenizer tokenizer;
    private final TextNormalizer textNormalizer;
    private final StopWordFilter stopWordFilter;
    private final PorterStemmerAdapter porterStemmerAdapter;
    private final InvertedIndex invertedIndex;
    private final CacheService cacheService;

    private final java.util.concurrent.atomic.AtomicLong indexingAttempts = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong successfulIndexOperations = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong failedIndexOperations = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalIndexLatencyMs = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong rebuildOperations = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong individualReindexOperations = new java.util.concurrent.atomic.AtomicLong(0);

    public IndexServiceImpl(
            DocumentService documentService,
            Tokenizer tokenizer,
            TextNormalizer textNormalizer,
            StopWordFilter stopWordFilter,
            PorterStemmerAdapter porterStemmerAdapter,
            InvertedIndex invertedIndex,
            CacheService cacheService) {
        this.documentService = documentService;
        this.tokenizer = tokenizer;
        this.textNormalizer = textNormalizer;
        this.stopWordFilter = stopWordFilter;
        this.porterStemmerAdapter = porterStemmerAdapter;
        this.invertedIndex = invertedIndex;
        this.cacheService = cacheService;
    }

    @Override
    public void indexDocument(UUID id) {
        indexingAttempts.incrementAndGet();
        long startTime = System.nanoTime();
        logger.info("Starting indexing for document ID: {}", id);
        DocumentResponse doc = documentService.retrieve(id); // Throws 404 if missing

        try {
            invertedIndex.removeDocument(id);

            if (doc.content() == null || doc.content().trim().isEmpty()) {
                logger.warn("Document ID: {} has empty content. Skipping safely.", id);
                documentService.updateStatus(id, DocumentStatus.ACTIVE, LocalDateTime.now());
                cacheService.clearAllSearchCaches();
                successfulIndexOperations.incrementAndGet();
                return;
            }

            List<String> tokens = tokenizer.tokenize(doc.content());
            invertedIndex.setDocumentLength(id, tokens.size());
            Map<String, Integer> termFrequencies = new HashMap<>();

            for (String token : tokens) {
                String normalized = textNormalizer.normalize(token);
                if (normalized.isEmpty()) {
                    continue;
                }

                if (stopWordFilter.isStopWord(normalized)) {
                    continue;
                }

                String stemmed = porterStemmerAdapter.stem(normalized);
                if (stemmed.isEmpty()) {
                    continue;
                }

                termFrequencies.put(stemmed, termFrequencies.getOrDefault(stemmed, 0) + 1);
            }

            for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
                invertedIndex.insert(entry.getKey(), new Posting(id, entry.getValue()));
            }

            documentService.updateStatus(id, DocumentStatus.ACTIVE, LocalDateTime.now());
            cacheService.clearAllSearchCaches();
            logger.info("Successfully indexed document ID: {}", id);
            successfulIndexOperations.incrementAndGet();
        } catch (TechAtlasException e) {
            failedIndexOperations.incrementAndGet();
            throw e;
        } catch (Exception e) {
            failedIndexOperations.incrementAndGet();
            logger.error("Failed to index document ID: {}", id, e);
            try {
                documentService.updateStatus(id, DocumentStatus.FAILED, null);
            } catch (Exception updateEx) {
                logger.error("Failed to update status to FAILED for document ID: {}", id, updateEx);
            }
            throw new TechAtlasException(HttpStatus.INTERNAL_SERVER_ERROR, "Indexing failed for document ID: " + id, e);
        } finally {
            long durationNs = System.nanoTime() - startTime;
            long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(durationNs);
            totalIndexLatencyMs.addAndGet(durationMs);
        }
    }

    @Override
    public void indexAll() {
        logger.info("Starting batch indexing for all pending/failed documents");
        List<DocumentResponse> documents = documentService.listAll();
        for (DocumentResponse doc : documents) {
            if (doc.status() == DocumentStatus.PENDING_INDEX || doc.status() == DocumentStatus.FAILED) {
                try {
                    indexDocument(doc.id());
                } catch (Exception e) {
                    logger.error("Failed to index document ID: {} during batch indexing", doc.id(), e);
                }
            }
        }
    }

    @Override
    public void rebuildIndex() {
        rebuildOperations.incrementAndGet();
        logger.info("Starting complete index rebuild");
        invertedIndex.clear();
        List<DocumentResponse> documents = documentService.listAll();
        for (DocumentResponse doc : documents) {
            if (doc.status() != DocumentStatus.DELETED) {
                try {
                    indexDocument(doc.id());
                } catch (Exception e) {
                    logger.error("Failed to index document ID: {} during index rebuild", doc.id(), e);
                }
            }
        }
        cacheService.clearAllSearchCaches();
    }

    @Override
    public void clearIndex() {
        logger.info("Clearing inverted index");
        invertedIndex.clear();
        cacheService.clearAllSearchCaches();
    }

    @Override
    public void removeDocument(UUID id) {
        logger.info("Removing document ID: {} from inverted index", id);
        invertedIndex.removeDocument(id);
        cacheService.clearAllSearchCaches();
    }

    @Override
    public void reindexDocument(UUID id) {
        individualReindexOperations.incrementAndGet();
        logger.info("Reindexing document ID: {}", id);
        indexDocument(id);
    }

    @Override
    public void indexPendingDocuments() {
        logger.info("Starting indexing for all pending documents");
        List<DocumentResponse> documents = documentService.listAll();
        int successCount = 0;
        int failCount = 0;
        for (DocumentResponse doc : documents) {
            if (doc.status() == DocumentStatus.PENDING_INDEX) {
                try {
                    indexDocument(doc.id());
                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to index pending document ID: {}", doc.id(), e);
                    failCount++;
                }
            }
        }
        logger.info("Pending indexing completed. Success: {}, Failed: {}", successCount, failCount);
    }

    @Override
    public long getIndexingAttempts() {
        return indexingAttempts.get();
    }

    @Override
    public long getSuccessfulIndexOperations() {
        return successfulIndexOperations.get();
    }

    @Override
    public long getFailedIndexOperations() {
        return failedIndexOperations.get();
    }

    @Override
    public double getAverageIndexLatencyMs() {
        long attempts = indexingAttempts.get();
        if (attempts == 0) {
            return 0.0;
        }
        return (double) totalIndexLatencyMs.get() / attempts;
    }

    @Override
    public long getRebuildOperations() {
        return rebuildOperations.get();
    }

    @Override
    public long getIndividualReindexOperations() {
        return individualReindexOperations.get();
    }
}
