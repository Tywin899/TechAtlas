package com.techatlas.fetcher.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubOwner(
    @JsonProperty("login") String login,
    @JsonProperty("id") Long id
) {}
