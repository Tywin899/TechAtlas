package com.techatlas.fetcher.wikipedia;

import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.fetcher.wikipedia.dto.WikipediaCategoryResponse;

public interface WikipediaClient {
    WikipediaPageSummary fetchPageSummary(String title);
    WikipediaCategoryResponse fetchCategoryMembers(String category, String continueToken);
}
