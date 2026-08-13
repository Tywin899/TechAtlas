package com.techatlas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GitHubDiscoverRequest(
    @NotBlank(message = "Search query is required")
    String query,

    @NotNull(message = "Max repositories limit is required")
    @Min(value = 1, message = "maxRepositories must be at least 1")
    @Max(value = 100, message = "maxRepositories must not exceed 100")
    Integer maxRepositories
) {}
