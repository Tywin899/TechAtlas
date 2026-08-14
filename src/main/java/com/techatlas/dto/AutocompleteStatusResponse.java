package com.techatlas.dto;

public record AutocompleteStatusResponse(
    boolean enabled,
    int vocabularySize,
    int prefixIndexTermCount,
    long totalSuggestionsRequests,
    int popularQueriesCount,
    int recentQueriesCount
) {}
