package com.techatlas.dto;

public record GitHubDiscoverResponse(
    String query,
    int repositoriesDiscovered,
    int repositoriesImported,
    int duplicatesSkipped
) {}
