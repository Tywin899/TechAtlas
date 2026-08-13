package com.techatlas.fetcher.stackoverflow;

import com.techatlas.config.StackOverflowProperties;
import com.techatlas.exception.StackOverflowMalformedResponseException;
import com.techatlas.exception.StackOverflowRateLimitExceededException;
import com.techatlas.exception.StackOverflowUnavailableException;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class StackOverflowClientImpl implements StackOverflowClient {

    private static final Logger logger = LoggerFactory.getLogger(StackOverflowClientImpl.class);

    private final RestClient restClient;
    private final StackOverflowProperties properties;

    public StackOverflowClientImpl(RestClient stackOverflowRestClient, StackOverflowProperties properties) {
        this.restClient = stackOverflowRestClient;
        this.properties = properties;
    }

    @Override
    public StackOverflowResponseWrapper<StackOverflowQuestionItem> searchQuestions(
            String query, List<String> tags, int page, int pageSize) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be blank");
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/search/advanced")
                    .queryParam("q", query.trim())
                    .queryParam("site", properties.getSite())
                    .queryParam("filter", "withbody")
                    .queryParam("page", page)
                    .queryParam("pagesize", pageSize);

            if (tags != null && !tags.isEmpty()) {
                uriBuilder.queryParam("tagged", String.join(";", tags));
            }

            String key = properties.getApiKey();
            if (key != null && !key.trim().isEmpty()) {
                uriBuilder.queryParam("key", key.trim());
            }

            String path = uriBuilder.build().toUriString();

            return restClient.get()
                    .uri(path)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int rawStatusCode = response.getStatusCode().value();
                        if (rawStatusCode == 403 || rawStatusCode == 429) {
                            throw new StackOverflowRateLimitExceededException("Stack Overflow API rate limit exceeded or access forbidden: " + rawStatusCode);
                        }
                        throw new StackOverflowUnavailableException("Stack Overflow client error searching questions: " + rawStatusCode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new StackOverflowUnavailableException("Stack Overflow server error: " + response.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<StackOverflowResponseWrapper<StackOverflowQuestionItem>>() {});
        } catch (StackOverflowRateLimitExceededException | StackOverflowUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error calling Stack Overflow Search API for [{}]: {}", query, e.getMessage());
            throw new StackOverflowUnavailableException("Timeout or network failure connecting to Stack Overflow: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error calling Stack Overflow Search API for [{}]: {}", query, e.getMessage());
            throw new StackOverflowMalformedResponseException("Unexpected error reading Stack Overflow search response: " + e.getMessage(), e);
        }
    }

    @Override
    public StackOverflowResponseWrapper<StackOverflowAnswerItem> fetchAnswers(Long questionId) {
        if (questionId == null) {
            throw new IllegalArgumentException("Question ID cannot be null");
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/questions/{id}/answers")
                    .queryParam("site", properties.getSite())
                    .queryParam("filter", "withbody");

            String key = properties.getApiKey();
            if (key != null && !key.trim().isEmpty()) {
                uriBuilder.queryParam("key", key.trim());
            }

            String path = uriBuilder.buildAndExpand(questionId).toUriString();

            return restClient.get()
                    .uri(path)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int rawStatusCode = response.getStatusCode().value();
                        if (rawStatusCode == 403 || rawStatusCode == 429) {
                            throw new StackOverflowRateLimitExceededException("Stack Overflow API rate limit exceeded or access forbidden: " + rawStatusCode);
                        }
                        throw new StackOverflowUnavailableException("Stack Overflow client error fetching answers: " + rawStatusCode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new StackOverflowUnavailableException("Stack Overflow server error fetching answers: " + response.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<StackOverflowResponseWrapper<StackOverflowAnswerItem>>() {});
        } catch (StackOverflowRateLimitExceededException | StackOverflowUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error calling Stack Overflow Answers API for question [{}]: {}", questionId, e.getMessage());
            throw new StackOverflowUnavailableException("Timeout or network failure connecting to Stack Overflow: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error calling Stack Overflow Answers API for question [{}]: {}", questionId, e.getMessage());
            throw new StackOverflowMalformedResponseException("Unexpected error reading Stack Overflow answers response: " + e.getMessage(), e);
        }
    }

    @Override
    public StackOverflowResponseWrapper<StackOverflowQuestionItem> fetchQuestion(Long questionId) {
        if (questionId == null) {
            throw new IllegalArgumentException("Question ID cannot be null");
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/questions/{id}")
                    .queryParam("site", properties.getSite())
                    .queryParam("filter", "withbody");

            String key = properties.getApiKey();
            if (key != null && !key.trim().isEmpty()) {
                uriBuilder.queryParam("key", key.trim());
            }

            String path = uriBuilder.buildAndExpand(questionId).toUriString();

            return restClient.get()
                    .uri(path)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int rawStatusCode = response.getStatusCode().value();
                        if (rawStatusCode == 403 || rawStatusCode == 429) {
                            throw new StackOverflowRateLimitExceededException("Stack Overflow API rate limit exceeded or access forbidden: " + rawStatusCode);
                        }
                        throw new StackOverflowUnavailableException("Stack Overflow client error fetching question details: " + rawStatusCode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new StackOverflowUnavailableException("Stack Overflow server error fetching question details: " + response.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<StackOverflowResponseWrapper<StackOverflowQuestionItem>>() {});
        } catch (StackOverflowRateLimitExceededException | StackOverflowUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error calling Stack Overflow Questions API for question [{}]: {}", questionId, e.getMessage());
            throw new StackOverflowUnavailableException("Timeout or network failure connecting to Stack Overflow: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error calling Stack Overflow Questions API for question [{}]: {}", questionId, e.getMessage());
            throw new StackOverflowMalformedResponseException("Unexpected error reading Stack Overflow question response: " + e.getMessage(), e);
        }
    }
}
