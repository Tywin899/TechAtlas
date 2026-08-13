package com.techatlas.fetcher.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubLicense(
    @JsonProperty("key") String key,
    @JsonProperty("name") String name,
    @JsonProperty("spdx_id") String spdxId
) {}
