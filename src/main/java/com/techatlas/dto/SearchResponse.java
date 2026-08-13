package com.techatlas.dto;

import java.util.List;

public record SearchResponse(
    String query,
    long totalResults,
    List<SearchResult> results,
    int page,
    int size,
    int totalPages
) {}
