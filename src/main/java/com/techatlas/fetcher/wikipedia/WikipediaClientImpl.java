package com.techatlas.fetcher.wikipedia;

import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.fetcher.wikipedia.dto.WikipediaCategoryResponse;
import com.techatlas.exception.WikipediaPageNotFoundException;
import com.techatlas.exception.WikipediaUnavailableException;
import com.techatlas.exception.WikipediaMalformedResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

@Component
public class WikipediaClientImpl implements WikipediaClient {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaClientImpl.class);
    private final RestClient restClient;

    public WikipediaClientImpl(RestClient wikipediaRestClient) {
        this.restClient = wikipediaRestClient;
    }

    @Override
    public WikipediaPageSummary fetchPageSummary(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Wikipedia page title cannot be blank");
        }

        try {
            return restClient.get()
                    .uri("/page/summary/{title}", title.trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new WikipediaPageNotFoundException("Wikipedia page not found: " + title);
                        }
                        throw new HttpClientErrorException(response.getStatusCode(), response.getStatusText());
                    })
                    .body(WikipediaPageSummary.class);
        } catch (WikipediaPageNotFoundException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            logger.error("Client error when calling Wikipedia API for title [{}]: {}", title, e.getMessage());
            if (e.getStatusCode().value() == 404) {
                throw new WikipediaPageNotFoundException("Wikipedia page not found: " + title);
            }
            throw new WikipediaMalformedResponseException("Wikipedia API client error: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error when calling Wikipedia API for title [{}]: {}", title, e.getMessage());
            throw new WikipediaUnavailableException("Wikipedia service is currently unavailable: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error when calling Wikipedia API for title [{}]: {}", title, e.getMessage());
            throw new WikipediaUnavailableException("Timeout or network failure connecting to Wikipedia: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error when calling Wikipedia API for title [{}]: {}", title, e.getMessage());
            throw new WikipediaMalformedResponseException("Unexpected error reading Wikipedia response: " + e.getMessage(), e);
        }
    }

    @Override
    public WikipediaCategoryResponse fetchCategoryMembers(String category, String continueToken) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Wikipedia category name cannot be blank");
        }

        String cmtitle = category.trim();
        if (!cmtitle.startsWith("Category:")) {
            cmtitle = "Category:" + cmtitle;
        }

        try {
            String uriString = "https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&format=json&cmlimit=500&cmtitle={cmtitle}";
            if (continueToken != null && !continueToken.isEmpty()) {
                uriString += "&cmcontinue={cmcontinue}";
            }

            RestClient.ResponseSpec responseSpec;
            if (continueToken != null && !continueToken.isEmpty()) {
                responseSpec = restClient.get()
                        .uri(uriString, cmtitle, continueToken)
                        .retrieve();
            } else {
                responseSpec = restClient.get()
                        .uri(uriString, cmtitle)
                        .retrieve();
            }

            return responseSpec
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new WikipediaUnavailableException("Wikipedia client error fetching category: " + response.getStatusCode());
                    })
                    .body(WikipediaCategoryResponse.class);
        } catch (WikipediaUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Network or timeout error when calling Wikipedia Category API for [{}]: {}", category, e.getMessage());
            throw new WikipediaUnavailableException("Timeout or network failure connecting to Wikipedia: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error when calling Wikipedia Category API for [{}]: {}", category, e.getMessage());
            throw new WikipediaMalformedResponseException("Unexpected error reading Wikipedia response: " + e.getMessage(), e);
        }
    }
}
