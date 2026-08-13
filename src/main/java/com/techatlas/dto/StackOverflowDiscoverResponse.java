package com.techatlas.dto;

public record StackOverflowDiscoverResponse(
    String query,
    int questionsDiscovered,
    int questionsImported,
    int duplicatesSkipped
) {}
