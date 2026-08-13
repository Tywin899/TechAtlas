package com.techatlas.service;

import com.techatlas.dto.SearchRequest;
import com.techatlas.dto.SearchResponse;

public interface SearchService {
    SearchResponse search(SearchRequest request);
}
