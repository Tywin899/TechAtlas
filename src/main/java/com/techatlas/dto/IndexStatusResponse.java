package com.techatlas.dto;

public record IndexStatusResponse(
    int indexedDocuments,
    int vocabularySize,
    int uniqueTerms,
    long totalPostings
) {}
