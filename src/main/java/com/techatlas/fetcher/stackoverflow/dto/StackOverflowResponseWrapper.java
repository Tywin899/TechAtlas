package com.techatlas.fetcher.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowResponseWrapper<T>(
    @JsonProperty("items") List<T> items,
    @JsonProperty("has_more") boolean hasMore,
    @JsonProperty("quota_max") int quotaMax,
    @JsonProperty("quota_remaining") int quotaRemaining
) {}
