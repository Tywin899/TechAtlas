package com.techatlas.fetcher.stackoverflow;

import com.techatlas.config.StackOverflowProperties;
import com.techatlas.exception.StackOverflowRateLimitExceededException;
import com.techatlas.exception.StackOverflowUnavailableException;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowResponseWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class StackOverflowClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private StackOverflowClient client;
    private StackOverflowProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StackOverflowProperties();
        properties.setApiUrl("https://api.stackexchange.com/2.3");
        properties.setSite("stackoverflow");

        restClientBuilder = RestClient.builder().baseUrl(properties.getApiUrl());
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new StackOverflowClientImpl(restClientBuilder.build(), properties);
    }

    @Test
    void testSearchQuestionsSuccess() {
        String jsonResponse = """
                {
                    "items": [
                        {
                            "question_id": 112233,
                            "title": "Spring Boot Dependency Injection",
                            "body": "<p>Body</p>",
                            "link": "https://stackoverflow.com/questions/112233",
                            "score": 10,
                            "tags": ["spring-boot"],
                            "is_answered": true,
                            "answer_count": 1
                        }
                    ],
                    "has_more": false
                }
                """;

        mockServer.expect(requestTo("https://api.stackexchange.com/2.3/search/advanced?q=spring-boot&site=stackoverflow&filter=withbody&page=1&pagesize=10&tagged=spring-boot"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        StackOverflowResponseWrapper<StackOverflowQuestionItem> response = client.searchQuestions(
                "spring-boot", List.of("spring-boot"), 1, 10
        );

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals("Spring Boot Dependency Injection", response.items().get(0).title());
    }

    @Test
    void testSearchQuestionsRateLimitError() {
        mockServer.expect(requestTo("https://api.stackexchange.com/2.3/search/advanced?q=spring-boot&site=stackoverflow&filter=withbody&page=1&pagesize=10"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(StackOverflowRateLimitExceededException.class, () ->
                client.searchQuestions("spring-boot", null, 1, 10));
    }

    @Test
    void testFetchAnswersSuccess() {
        String jsonResponse = """
                {
                    "items": [
                        {
                            "answer_id": 556677,
                            "body": "<p>Use Autowired annotation</p>",
                            "score": 5,
                            "is_accepted": true
                        }
                    ],
                    "has_more": false
                }
                """;

        mockServer.expect(requestTo("https://api.stackexchange.com/2.3/questions/112233/answers?site=stackoverflow&filter=withbody"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        StackOverflowResponseWrapper<StackOverflowAnswerItem> response = client.fetchAnswers(112233L);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertTrue(response.items().get(0).isAccepted());
    }
}
