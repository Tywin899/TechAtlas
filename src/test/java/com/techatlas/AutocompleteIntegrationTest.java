package com.techatlas;

import com.techatlas.autocomplete.QueryTracker;
import com.techatlas.autocomplete.service.AutocompleteService;
import com.techatlas.dto.*;
import com.techatlas.entity.SourceType;
import com.techatlas.index.IndexService;
import com.techatlas.model.InvertedIndex;
import com.techatlas.service.DocumentService;
import com.techatlas.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AutocompleteIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private IndexService indexService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private InvertedIndex invertedIndex;

    @Autowired
    private AutocompleteService autocompleteService;

    @Autowired
    private QueryTracker queryTracker;

    @BeforeEach
    public void cleanUp() {
        invertedIndex.clear();
        queryTracker.clearFallbackData();
        // Delete all DB documents if any
        documentService.listAll().forEach(doc -> documentService.delete(doc.id()));
    }

    @Test
    public void testFullAutocompleteLifecycle() {
        // 1. Create and Index documents
        CreateDocumentRequest doc1 = new CreateDocumentRequest(
                "Java Intro",
                "Learn Java programming language basics.",
                "http://url1.com",
                SourceType.MANUAL,
                "java",
                "author",
                "en",
                null
        );
        CreateDocumentRequest doc2 = new CreateDocumentRequest(
                "Spring In Action",
                "Spring Boot framework simplifies Java application development.",
                "http://url2.com",
                SourceType.MANUAL,
                "spring",
                "author",
                "en",
                null
        );

        DocumentResponse res1 = documentService.create(doc1);
        DocumentResponse res2 = documentService.create(doc2);

        indexService.indexDocument(res1.id());
        indexService.indexDocument(res2.id());

        // 2. Query suggestions for "spr" prefix
        AutocompleteResponse suggestions = autocompleteService.getSuggestions("spr", 5);
        assertThat(suggestions.suggestions()).isNotEmpty();
        // Should contain "spring" and "springboot" or similar terms (depending on stemming/tokenization)
        List<String> matchedTexts = suggestions.suggestions().stream().map(SuggestionItem::text).toList();
        assertThat(matchedTexts).contains("spring");

        // 3. Perform a Search query to track popularity
        SearchRequest searchRequest = new SearchRequest("spring boot", 0, 10);
        searchService.search(searchRequest);

        // 4. Request suggestions again, verify "spring boot" is recommended as a QUERY type suggestion
        AutocompleteResponse suggestions2 = autocompleteService.getSuggestions("spr", 5);
        List<SuggestionItem> items2 = suggestions2.suggestions();
        assertThat(items2).hasSizeGreaterThanOrEqualTo(1);

        boolean hasQueryType = items2.stream().anyMatch(item -> "spring boot".equals(item.text()) && "QUERY".equals(item.type()));
        assertThat(hasQueryType).isTrue();

        // 5. Delete document and verify terms disappear
        documentService.delete(res2.id());
        // Incremental maintenance should evict the deleted document from the index
        // Vocabulary terms specific to doc2 only (like "simplifies" or "framework") should be removed from prefix index
        AutocompleteResponse suggestionsDeleted = autocompleteService.getSuggestions("simpl", 5);
        assertThat(suggestionsDeleted.suggestions()).isEmpty();

        // 6. Run full index rebuild and verify autocomplete remains synchronized
        invertedIndex.clear();
        indexService.rebuildIndex();
        
        AutocompleteResponse suggestionsRebuilt = autocompleteService.getSuggestions("java", 5);
        assertThat(suggestionsRebuilt.suggestions()).isNotEmpty();
    }
}
