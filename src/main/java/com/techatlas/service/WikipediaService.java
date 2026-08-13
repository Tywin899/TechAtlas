package com.techatlas.service;

import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.WikipediaDiscoverRequest;
import com.techatlas.dto.WikipediaDiscoverResponse;
import com.techatlas.dto.WikipediaSyncStatusResponse;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;

public interface WikipediaService {
    WikipediaPageSummary fetchSummary(String title);
    DocumentResponse importArticle(String title);
    DocumentResponse importArticle(String title, String category);
    WikipediaDiscoverResponse discoverArticles(WikipediaDiscoverRequest request);
    WikipediaSyncStatusResponse getSyncStatus();
}
