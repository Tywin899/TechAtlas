package com.techatlas.mapper;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.entity.SourceType;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WikipediaMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WikipediaMapper wikipediaMapper = new WikipediaMapper(objectMapper);

    @Test
    void testToCreateRequest() {
        WikipediaPageSummary.ContentUrls contentUrls = new WikipediaPageSummary.ContentUrls(
                new WikipediaPageSummary.ContentUrls.Desktop("https://en.wikipedia.org/wiki/Java"),
                new WikipediaPageSummary.ContentUrls.Mobile("https://en.m.wikipedia.org/wiki/Java")
        );
        WikipediaPageSummary.Thumbnail thumbnail = new WikipediaPageSummary.Thumbnail(
                "https://example.com/thumb.png", 100, 100
        );

        WikipediaPageSummary summary = new WikipediaPageSummary(
                "Java",
                "Java is a high-level programming language.",
                "Programming language",
                12345L,
                "en",
                "987654",
                contentUrls,
                thumbnail
        );

        CreateDocumentRequest request = wikipediaMapper.toCreateRequest(summary);

        assertNotNull(request);
        assertEquals("Java", request.title());
        assertEquals("Java is a high-level programming language.", request.content());
        assertEquals("https://en.wikipedia.org/wiki/Java", request.url());
        assertEquals(SourceType.WIKIPEDIA, request.source());
        assertNull(request.category());
        assertEquals("Wikipedia", request.author());
        assertEquals("en", request.language());
        
        assertNotNull(request.metadata());
        assertTrue(request.metadata().contains("12345"));
        assertTrue(request.metadata().contains("Programming language"));
        assertTrue(request.metadata().contains("987654"));
        assertTrue(request.metadata().contains("https://example.com/thumb.png"));
    }

    @Test
    void testToCreateRequestNull() {
        assertNull(wikipediaMapper.toCreateRequest(null));
    }

    @Test
    void testToCreateRequestWithCategory() {
        WikipediaPageSummary summary = new WikipediaPageSummary(
                "Java",
                "Java is a programming language.",
                "OOP language",
                12345L,
                "en",
                "1",
                new WikipediaPageSummary.ContentUrls(
                        new WikipediaPageSummary.ContentUrls.Desktop("https://en.wikipedia.org/wiki/Java"),
                        null
                ),
                null
        );

        CreateDocumentRequest request = wikipediaMapper.toCreateRequest(summary, "Java (programming language)");

        assertNotNull(request);
        assertEquals("Java", request.title());
        assertEquals("Java (programming language)", request.category());
    }
}
