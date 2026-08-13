package com.techatlas.fetcher.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GitHubRepoItem(
    @JsonProperty("id") Long id,
    @JsonProperty("name") String name,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("description") String description,
    @JsonProperty("html_url") String htmlUrl,
    @JsonProperty("stargazers_count") int stargazersCount,
    @JsonProperty("forks_count") int forksCount,
    @JsonProperty("language") String language,
    @JsonProperty("owner") GitHubOwner owner,
    @JsonProperty("topics") List<String> topics,
    @JsonProperty("default_branch") String defaultBranch,
    @JsonProperty("license") GitHubLicense license
) {}
