package com.techatlas.fetcher.wikipedia.dto;

import jakarta.validation.constraints.NotBlank;

public record WikipediaImportRequest(
    @NotBlank(message = "Wikipedia article title is required")
    String title
) {}
