package com.techatlas.model;

import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InvertedIndex {
    private final Map<String, PostingList> index = new ConcurrentHashMap<>();
    private final Set<UUID> indexedDocuments = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> documentLengths = new ConcurrentHashMap<>();

    public synchronized void insert(String term, Posting posting) {
        if (term == null || term.isBlank() || posting == null) {
            return;
        }
        index.computeIfAbsent(term, k -> new PostingList()).addPosting(posting);
        indexedDocuments.add(posting.documentId());
    }

    public PostingList retrieve(String term) {
        if (term == null) {
            return null;
        }
        return index.get(term);
    }

    public synchronized void clear() {
        index.clear();
        indexedDocuments.clear();
        documentLengths.clear();
    }

    public int getDocumentCount() {
        return indexedDocuments.size();
    }

    public int getVocabularySize() {
        return index.size();
    }

    public synchronized void removeDocument(UUID documentId) {
        if (documentId == null) {
            return;
        }
        if (indexedDocuments.remove(documentId)) {
            for (PostingList postingList : index.values()) {
                postingList.removePostingForDocument(documentId);
            }
            index.entrySet().removeIf(entry -> entry.getValue().getPostings().isEmpty());
            documentLengths.remove(documentId);
        }
    }

    public synchronized void setDocumentLength(UUID documentId, int length) {
        if (documentId != null) {
            documentLengths.put(documentId, length);
        }
    }

    public int getDocumentLength(UUID documentId) {
        if (documentId == null) {
            return 0;
        }
        return documentLengths.getOrDefault(documentId, 0);
    }

    public synchronized double getAverageDocumentLength() {
        int size = documentLengths.size();
        if (size == 0) {
            return 0.0;
        }
        long totalLength = 0;
        for (int length : documentLengths.values()) {
            totalLength += length;
        }
        return (double) totalLength / size;
    }

    public Map<String, PostingList> getIndex() {
        return Collections.unmodifiableMap(index);
    }
}
