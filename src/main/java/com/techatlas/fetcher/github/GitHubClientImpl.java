package com.techatlas.fetcher.github;

import com.techatlas.exception.GitHubMalformedResponseException;
import com.techatlas.exception.GitHubRateLimitExceededException;
import com.techatlas.exception.GitHubUnavailableException;
import com.techatlas.fetcher.github.dto.GitHubReadmeResponse;
import com.techatlas.fetcher.github.dto.GitHubRepoItem;
import com.techatlas.fetcher.github.dto.GitHubSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class GitHubClientImpl implements GitHubClient {

    private static final Logger logger = LoggerFactory.getLogger(GitHubClientImpl.class);
    private final RestClient restClient;

    public GitHubClientImpl(RestClient githubRestClient) {
        this.restClient = githubRestClient;
    }

    @Override
    public GitHubSearchResponse searchRepositories(String query, int page, int perPage) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be blank");
        }

        try {
            return restClient.get()
                    .uri("/search/repositories?q={q}&page={page}&per_page={per_page}", query.trim(), page, perPage)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int rawStatusCode = response.getStatusCode().value();
                        if (rawStatusCode == 403 || rawStatusCode == 429) {
                            throw new GitHubRateLimitExceededException("GitHub API rate limit exceeded or access forbidden: " + rawStatusCode);
                        }
                        throw new GitHubUnavailableException("GitHub client error searching repositories: " + rawStatusCode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new GitHubUnavailableException("GitHub server error: " + response.getStatusCode());
                    })
                    .body(GitHubSearchResponse.class);
        } catch (GitHubRateLimitExceededException | GitHubUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error when calling GitHub Search API for [{}]: {}", query, e.getMessage());
            throw new GitHubUnavailableException("Timeout or network failure connecting to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error when calling GitHub Search API for [{}]: {}", query, e.getMessage());
            throw new GitHubMalformedResponseException("Unexpected error reading GitHub search response: " + e.getMessage(), e);
        }
    }

    @Override
    public GitHubReadmeResponse fetchReadme(String owner, String repo) {
        if (owner == null || owner.trim().isEmpty() || repo == null || repo.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository owner and name cannot be blank");
        }

        try {
            return restClient.get()
                    .uri("/repos/{owner}/{repo}/readme", owner.trim(), repo.trim())
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "README not found");
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int rawStatusCode = response.getStatusCode().value();
                        if (rawStatusCode == 403 || rawStatusCode == 429) {
                            throw new GitHubRateLimitExceededException("GitHub API rate limit exceeded or access forbidden: " + rawStatusCode);
                        }
                        throw new GitHubUnavailableException("GitHub client error fetching readme: " + rawStatusCode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new GitHubUnavailableException("GitHub server error fetching readme: " + response.getStatusCode());
                    })
                    .body(GitHubReadmeResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.debug("README not found for repo {}/{}", owner, repo);
                return null;
            }
            logger.error("GitHub client error when calling GitHub Readme API for {}/{}: {}", owner, repo, e.getMessage());
            throw new GitHubUnavailableException("GitHub client error: " + e.getMessage(), e);
        } catch (GitHubRateLimitExceededException | GitHubUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error when calling GitHub Readme API for {}/{}: {}", owner, repo, e.getMessage());
            throw new GitHubUnavailableException("Timeout or network failure connecting to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error when calling GitHub Readme API for {}/{}: {}", owner, repo, e.getMessage());
            throw new GitHubMalformedResponseException("Unexpected error reading GitHub readme response: " + e.getMessage(), e);
        }
    }

    @Override
    public GitHubRepoItem fetchRepository(String owner, String repo) {
        if (owner == null || owner.trim().isEmpty() || repo == null || repo.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository owner and name cannot be blank");
        }

        try {
            return restClient.get()
                    .uri("/repos/{owner}/{repo}", owner.trim(), repo.trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int rawStatusCode = response.getStatusCode().value();
                        if (rawStatusCode == 403 || rawStatusCode == 429) {
                            throw new GitHubRateLimitExceededException("GitHub API rate limit exceeded or access forbidden: " + rawStatusCode);
                        }
                        throw new GitHubUnavailableException("GitHub client error fetching repository details: " + rawStatusCode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new GitHubUnavailableException("GitHub server error fetching repository details: " + response.getStatusCode());
                    })
                    .body(GitHubRepoItem.class);
        } catch (GitHubRateLimitExceededException | GitHubUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error when calling GitHub Repos API for {}/{}: {}", owner, repo, e.getMessage());
            throw new GitHubUnavailableException("Timeout or network failure connecting to GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error when calling GitHub Repos API for {}/{}: {}", owner, repo, e.getMessage());
            throw new GitHubMalformedResponseException("Unexpected error reading GitHub repo response: " + e.getMessage(), e);
        }
    }
}
