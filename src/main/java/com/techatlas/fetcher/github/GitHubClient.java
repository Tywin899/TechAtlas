package com.techatlas.fetcher.github;

import com.techatlas.fetcher.github.dto.GitHubReadmeResponse;
import com.techatlas.fetcher.github.dto.GitHubRepoItem;
import com.techatlas.fetcher.github.dto.GitHubSearchResponse;

public interface GitHubClient {
    GitHubSearchResponse searchRepositories(String query, int page, int perPage);
    GitHubReadmeResponse fetchReadme(String owner, String repo);
    GitHubRepoItem fetchRepository(String owner, String repo);
}
