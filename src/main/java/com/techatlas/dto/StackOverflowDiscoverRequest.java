package com.techatlas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StackOverflowDiscoverRequest(
    @NotBlank(message = "Search query is required")
    String query,

    List<String> tags,

    @NotNull(message = "Max questions limit is required")
    @Min(value = 1, message = "maxQuestions must be at least 1")
    @Max(value = 100, message = "maxQuestions must not exceed 100")
    Integer maxQuestions
) {}
