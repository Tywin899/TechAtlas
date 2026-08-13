package com.techatlas.dto;

public record WikipediaDiscoverResponse(
    String category,
    int articlesDiscovered,
    int articlesImported,
    int duplicatesSkipped,
    int categoriesVisited
) {}
