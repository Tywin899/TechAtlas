package com.techatlas.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface CacheService {
    boolean isEnabled();
    boolean isAvailable();
    Optional<Object> get(String key);
    void put(String key, Object value, long ttl, TimeUnit unit);
    void evict(String key);
    void clearPattern(String pattern);
    void clearAllSearchCaches();

    // Observability metrics
    long getSearchHits();
    long getSearchMisses();
    long getDocumentHits();
    long getDocumentMisses();
    long getEvictions();

    void incrementSearchHits();
    void incrementSearchMisses();
    void incrementDocumentHits();
    void incrementDocumentMisses();
    void incrementEvictions();
}
