package com.techatlas.fetcher.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubReadmeResponse(
    @JsonProperty("name") String name,
    @JsonProperty("path") String path,
    @JsonProperty("sha") String sha,
    @JsonProperty("size") long size,
    @JsonProperty("content") String content,
    @JsonProperty("encoding") String encoding
) {}
