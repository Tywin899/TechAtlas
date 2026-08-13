package com.techatlas.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowAnswerItem;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowOwner;
import com.techatlas.fetcher.stackoverflow.dto.StackOverflowQuestionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StackOverflowMapperTest {

    private StackOverflowMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new StackOverflowMapper(new ObjectMapper());
    }

    @Test
    void testToCreateRequest() {
        StackOverflowQuestionItem question = new StackOverflowQuestionItem(
                112233L,
                "Spring Boot",
                "<p>Question Body</p>",
                "https://stackoverflow.com/questions/112233",
                10,
                List.of("spring-boot", "java"),
                new StackOverflowOwner("John Doe", 99L),
                true,
                1,
                5566L,
                1628859600L
        );

        StackOverflowAnswerItem answer = new StackOverflowAnswerItem(
                5566L,
                "<p>Answer Body</p>",
                20,
                true,
                new StackOverflowOwner("Jane Smith", 88L)
        );

        CreateDocumentRequest request = mapper.toCreateRequest(question, List.of(answer), "spring-boot");

        assertNotNull(request);
        assertEquals("Spring Boot", request.title());
        assertTrue(request.content().contains("Question Body"));
        assertTrue(request.content().contains("Answer Body"));
        assertTrue(request.content().contains("Jane Smith"));
        assertEquals("https://stackoverflow.com/questions/112233", request.url());
        assertEquals("John Doe", request.author());
        assertEquals(SourceType.STACKOVERFLOW, request.source());
        assertEquals("spring-boot", request.category());
        assertEquals("en", request.language());
        assertNotNull(request.metadata());
        assertTrue(request.metadata().contains("112233"));
    }
}
