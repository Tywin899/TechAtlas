package com.techatlas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WikipediaDiscoverRequest(
    @NotBlank(message = "Category name is required")
    String category,

    @NotNull(message = "Max articles limit is required")
    @Min(value = 1, message = "maxArticles must be at least 1")
    @Max(value = 500, message = "maxArticles must not exceed 500")
    Integer maxArticles,

    @NotNull(message = "Max depth is required")
    @Min(value = 0, message = "maxDepth must be non-negative")
    @Max(value = 5, message = "maxDepth must not exceed 5")
    Integer maxDepth
) {}
