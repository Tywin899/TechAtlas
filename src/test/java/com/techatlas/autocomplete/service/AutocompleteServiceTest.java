package com.techatlas.autocomplete.service;

import com.techatlas.autocomplete.QueryTracker;
import com.techatlas.config.AutocompleteProperties;
import com.techatlas.dto.AutocompleteResponse;
import com.techatlas.dto.SuggestionItem;
import com.techatlas.model.InvertedIndex;
import com.techatlas.model.Posting;
import com.techatlas.model.PostingList;
import com.techatlas.model.PrefixTrie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutocompleteServiceTest {

    @Mock
    private InvertedIndex invertedIndex;

    @Mock
    private QueryTracker queryTracker;

    private PrefixTrie prefixTrie;
    private AutocompleteProperties properties;
    private AutocompleteService autocompleteService;

    @BeforeEach
    public void setUp() {
        prefixTrie = new PrefixTrie();
        properties = new AutocompleteProperties();
        properties.setDefaultLimit(5);
        properties.setMaxLimit(10);
        properties.setMaxPrefixLength(20);

        lenient().when(invertedIndex.getPrefixTrie()).thenReturn(prefixTrie);

        autocompleteService = new AutocompleteServiceImpl(invertedIndex, queryTracker, properties);
    }

    @Test
    public void testGetSuggestionsForPrefixMatchesAndSorts() {
        prefixTrie.insert("spring");
        prefixTrie.insert("springboot");

        // mock PostingLists
        PostingList p1 = new PostingList();
        p1.addPosting(new Posting(UUID.randomUUID(), 10));
        PostingList p2 = new PostingList();
        p2.addPosting(new Posting(UUID.randomUUID(), 5));

        when(invertedIndex.retrieve("spring")).thenReturn(p1);
        when(invertedIndex.retrieve("springboot")).thenReturn(p2);

        // mock popular queries
        when(queryTracker.getPopularQueries()).thenReturn(Map.of(
                "spring boot", 15.0,
                "java", 8.0
        ));

        AutocompleteResponse response = autocompleteService.getSuggestions("spr", 5);

        assertThat(response.query()).isEqualTo("spr");
        // Suggestions should be sorted:
        // 1. "spring boot" (QUERY, frequency 15)
        // 2. "spring" (TERM, frequency 10)
        // 3. "springboot" (TERM, frequency 5)
        assertThat(response.suggestions()).hasSize(3);
        assertThat(response.suggestions().get(0).text()).isEqualTo("spring boot");
        assertThat(response.suggestions().get(0).type()).isEqualTo("QUERY");
        assertThat(response.suggestions().get(0).frequency()).isEqualTo(15L);

        assertThat(response.suggestions().get(1).text()).isEqualTo("spring");
        assertThat(response.suggestions().get(1).type()).isEqualTo("TERM");

        assertThat(response.suggestions().get(2).text()).isEqualTo("springboot");
        assertThat(response.suggestions().get(2).type()).isEqualTo("TERM");
    }

    @Test
    public void testGetSuggestionsForBlankQueryReturnsRecentAndPopular() {
        when(queryTracker.getRecentQueries()).thenReturn(List.of("java recent"));
        when(queryTracker.getPopularQueries()).thenReturn(Map.of("spring popular", 50.0));

        AutocompleteResponse response = autocompleteService.getSuggestions(" ", 5);

        assertThat(response.suggestions()).hasSize(2);
        assertThat(response.suggestions().get(0).text()).isEqualTo("java recent");
        assertThat(response.suggestions().get(0).type()).isEqualTo("RECENT");

        assertThat(response.suggestions().get(1).text()).isEqualTo("spring popular");
        assertThat(response.suggestions().get(1).type()).isEqualTo("QUERY");
    }
}
