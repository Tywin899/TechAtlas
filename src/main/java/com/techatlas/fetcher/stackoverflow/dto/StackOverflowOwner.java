package com.techatlas.fetcher.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackOverflowOwner(
    @JsonProperty("display_name") String displayName,
    @JsonProperty("user_id") Long userId
) {}
