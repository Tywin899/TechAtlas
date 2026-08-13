package com.techatlas.index;

import java.util.UUID;

public interface IndexService {
    void indexDocument(UUID id);
    void indexAll();
    void rebuildIndex();
    void clearIndex();
    void removeDocument(UUID id);
    void reindexDocument(UUID id);
    void indexPendingDocuments();
}
