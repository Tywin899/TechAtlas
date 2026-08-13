package com.techatlas.fetcher.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GitHubSearchResponse(
    @JsonProperty("total_count") int totalCount,
    @JsonProperty("incomplete_results") boolean incompleteResults,
    @JsonProperty("items") List<GitHubRepoItem> items
) {}
