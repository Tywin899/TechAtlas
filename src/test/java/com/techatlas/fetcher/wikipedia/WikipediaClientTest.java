package com.techatlas.fetcher.wikipedia;

import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.fetcher.wikipedia.dto.WikipediaCategoryResponse;
import com.techatlas.exception.WikipediaPageNotFoundException;
import com.techatlas.exception.WikipediaUnavailableException;
import com.techatlas.exception.WikipediaMalformedResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WikipediaClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private WikipediaClient wikipediaClient;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("https://en.wikipedia.org/api/rest_v1");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        wikipediaClient = new WikipediaClientImpl(restClientBuilder.build());
    }

    @Test
    void testFetchPageSummarySuccess() {
        String jsonResponse = """
                {
                    "title": "Spring Framework",
                    "extract": "Spring is an application framework.",
                    "description": "Java framework",
                    "pageid": 54321,
                    "lang": "en",
                    "revision": "123456",
                    "content_urls": {
                        "desktop": {
                            "page": "https://en.wikipedia.org/wiki/Spring_Framework"
                        }
                    }
                }
                """;

        mockServer.expect(requestTo("https://en.wikipedia.org/api/rest_v1/page/summary/Spring_Framework"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        WikipediaPageSummary summary = wikipediaClient.fetchPageSummary("Spring_Framework");

        assertNotNull(summary);
        assertEquals("Spring Framework", summary.title());
        assertEquals("Spring is an application framework.", summary.extract());
        assertEquals("Java framework", summary.description());
        assertEquals(54321L, summary.pageId());
        assertEquals("en", summary.lang());
        assertEquals("123456", summary.revision());
        assertNotNull(summary.contentUrls());
        assertEquals("https://en.wikipedia.org/wiki/Spring_Framework", summary.contentUrls().desktop().page());
    }

    @Test
    void testFetchPageSummaryNotFoundThrows404() {
        mockServer.expect(requestTo("https://en.wikipedia.org/api/rest_v1/page/summary/Nonexistent_Page"))
                .andRespond(withResourceNotFound());

        assertThrows(WikipediaPageNotFoundException.class,
                () -> wikipediaClient.fetchPageSummary("Nonexistent_Page"));
    }

    @Test
    void testFetchPageSummaryTimeoutThrows503() {
        mockServer.expect(requestTo("https://en.wikipedia.org/api/rest_v1/page/summary/Timeout_Page"))
                .andRespond(withException(new java.io.IOException("Connection timeout")));

        assertThrows(WikipediaUnavailableException.class,
                () -> wikipediaClient.fetchPageSummary("Timeout_Page"));
    }

    @Test
    void testFetchPageSummaryServerErrorThrows503() {
        mockServer.expect(requestTo("https://en.wikipedia.org/api/rest_v1/page/summary/Error_Page"))
                .andRespond(withServerError());

        assertThrows(WikipediaUnavailableException.class,
                () -> wikipediaClient.fetchPageSummary("Error_Page"));
    }

    @Test
    void testFetchPageSummaryMalformedResponseThrows500() {
        mockServer.expect(requestTo("https://en.wikipedia.org/api/rest_v1/page/summary/Malformed_Page"))
                .andRespond(withSuccess("{ invalid json ", MediaType.APPLICATION_JSON));

        assertThrows(WikipediaMalformedResponseException.class,
                () -> wikipediaClient.fetchPageSummary("Malformed_Page"));
    }

    @Test
    void testFetchCategoryMembersSuccess() {
        String jsonResponse = """
                {
                    "continue": {
                        "cmcontinue": "page|2d2d|456",
                        "continue": "-||"
                    },
                    "query": {
                        "categorymembers": [
                          { "pageid": 1, "ns": 0, "title": "Java Language" },
                          { "pageid": 2, "ns": 14, "title": "Category:Java tools" }
                        ]
                    }
                }
                """;

        mockServer.expect(requestTo("https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&format=json&cmlimit=500&cmtitle=Category%3AJava_programming_language"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        WikipediaCategoryResponse response = wikipediaClient.fetchCategoryMembers("Java_programming_language", null);

        assertNotNull(response);
        assertNotNull(response.continueToken());
        assertEquals("page|2d2d|456", response.continueToken().cmcontinue());
        assertNotNull(response.query());
        assertEquals(2, response.query().categorymembers().size());
        assertEquals("Java Language", response.query().categorymembers().get(0).title());
        assertEquals(0, response.query().categorymembers().get(0).ns());
        assertEquals("Category:Java tools", response.query().categorymembers().get(1).title());
        assertEquals(14, response.query().categorymembers().get(1).ns());
    }

    @Test
    void testFetchCategoryMembersWithContinue() {
        String jsonResponse = """
                {
                    "query": {
                        "categorymembers": [
                          { "pageid": 3, "ns": 0, "title": "Kotlin" }
                        ]
                    }
                }
                """;

        mockServer.expect(requestTo("https://en.wikipedia.org/w/api.php?action=query&list=categorymembers&format=json&cmlimit=500&cmtitle=Category%3AJava_programming_language&cmcontinue=page%7C2d2d%7C456"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        WikipediaCategoryResponse response = wikipediaClient.fetchCategoryMembers("Java_programming_language", "page|2d2d|456");

        assertNotNull(response);
        assertNull(response.continueToken());
        assertNotNull(response.query());
        assertEquals(1, response.query().categorymembers().size());
        assertEquals("Kotlin", response.query().categorymembers().get(0).title());
    }
}
