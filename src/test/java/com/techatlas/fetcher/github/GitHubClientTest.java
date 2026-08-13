package com.techatlas.fetcher.github;

import com.techatlas.exception.GitHubMalformedResponseException;
import com.techatlas.exception.GitHubRateLimitExceededException;
import com.techatlas.exception.GitHubUnavailableException;
import com.techatlas.fetcher.github.dto.GitHubReadmeResponse;
import com.techatlas.fetcher.github.dto.GitHubSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GitHubClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private GitHubClient githubClient;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("https://api.github.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        githubClient = new GitHubClientImpl(restClientBuilder.build());
    }

    @Test
    void testSearchRepositoriesSuccess() {
        String jsonResponse = """
                {
                    "total_count": 1,
                    "incomplete_results": false,
                    "items": [
                        {
                            "id": 123456,
                            "name": "spring-boot",
                            "full_name": "spring-projects/spring-boot",
                            "description": "Spring Boot description",
                            "html_url": "https://github.com/spring-projects/spring-boot",
                            "stargazers_count": 100,
                            "forks_count": 50,
                            "language": "Java",
                            "owner": {
                                "login": "spring-projects",
                                "id": 9876
                            },
                            "topics": ["spring", "java"],
                            "default_branch": "main"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=spring-boot&page=1&per_page=10"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubSearchResponse response = githubClient.searchRepositories("spring-boot", 1, 10);

        assertNotNull(response);
        assertEquals(1, response.totalCount());
        assertFalse(response.incompleteResults());
        assertEquals(1, response.items().size());
        assertEquals("spring-projects/spring-boot", response.items().get(0).fullName());
    }

    @Test
    void testSearchRepositoriesRateLimitError() {
        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=spring-boot&page=1&per_page=10"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(GitHubRateLimitExceededException.class, () -> 
                githubClient.searchRepositories("spring-boot", 1, 10));
    }

    @Test
    void testFetchReadmeSuccess() {
        String jsonResponse = """
                {
                    "name": "README.md",
                    "path": "README.md",
                    "sha": "sha123",
                    "size": 100,
                    "content": "SGVsbG8gV29ybGQ=",
                    "encoding": "base64"
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/spring-projects/spring-boot/readme"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubReadmeResponse response = githubClient.fetchReadme("spring-projects", "spring-boot");

        assertNotNull(response);
        assertEquals("README.md", response.name());
        assertEquals("SGVsbG8gV29ybGQ=", response.content());
    }

    @Test
    void testFetchReadmeNotFoundReturnsNull() {
        mockServer.expect(requestTo("https://api.github.com/repos/spring-projects/spring-boot/readme"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        GitHubReadmeResponse response = githubClient.fetchReadme("spring-projects", "spring-boot");

        assertNull(response);
    }
}
