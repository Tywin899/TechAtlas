package com.techatlas.autocomplete.service;

import com.techatlas.autocomplete.QueryTracker;
import com.techatlas.config.AutocompleteProperties;
import com.techatlas.dto.AutocompleteResponse;
import com.techatlas.dto.AutocompleteStatusResponse;
import com.techatlas.dto.SuggestionItem;
import com.techatlas.model.InvertedIndex;
import com.techatlas.model.Posting;
import com.techatlas.model.PostingList;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AutocompleteServiceImpl implements AutocompleteService {

    private final InvertedIndex invertedIndex;
    private final QueryTracker queryTracker;
    private final AutocompleteProperties properties;
    private final AtomicLong totalRequests = new AtomicLong(0);

    public AutocompleteServiceImpl(InvertedIndex invertedIndex,
                                   QueryTracker queryTracker,
                                   AutocompleteProperties properties) {
        this.invertedIndex = invertedIndex;
        this.queryTracker = queryTracker;
        this.properties = properties;
    }

    @Override
    public AutocompleteResponse getSuggestions(String query, Integer limit) {
        totalRequests.incrementAndGet();

        if (!properties.isEnabled()) {
            return new AutocompleteResponse(query, Collections.emptyList(), 0);
        }

        int resolvedLimit = limit != null ? limit : properties.getDefaultLimit();
        if (resolvedLimit <= 0) {
            resolvedLimit = properties.getDefaultLimit();
        } else if (resolvedLimit > properties.getMaxLimit()) {
            resolvedLimit = properties.getMaxLimit();
        }

        if (query == null || query.isBlank()) {
            // Retrieve recent queries and top popular queries when query is blank
            List<SuggestionItem> blankSuggestions = new ArrayList<>();

            List<String> recent = queryTracker.getRecentQueries();
            for (String r : recent) {
                blankSuggestions.add(new SuggestionItem(r, "RECENT", 0));
            }

            Map<String, Double> popular = queryTracker.getPopularQueries();
            for (Map.Entry<String, Double> entry : popular.entrySet()) {
                // Avoid duplicating if it is already in recent queries
                if (recent.contains(entry.getKey())) {
                    continue;
                }
                blankSuggestions.add(new SuggestionItem(entry.getKey(), "QUERY", entry.getValue().longValue()));
            }

            List<SuggestionItem> sliced = blankSuggestions.stream()
                    .limit(resolvedLimit)
                    .collect(Collectors.toList());

            return new AutocompleteResponse("", sliced, sliced.size());
        }

        String normalized = normalizePrefix(query);
        if (normalized.length() > properties.getMaxPrefixLength()) {
            normalized = normalized.substring(0, properties.getMaxPrefixLength());
        }

        List<SuggestionItem> suggestions = new ArrayList<>();

        // 1. Vocabulary prefix matching
        List<String> matchedTerms = invertedIndex.getPrefixTrie().prefixLookup(normalized);
        for (String term : matchedTerms) {
            PostingList plist = invertedIndex.retrieve(term);
            long frequency = 0;
            if (plist != null) {
                frequency = plist.getPostings().stream().mapToLong(Posting::termFrequency).sum();
            }
            suggestions.add(new SuggestionItem(term, "TERM", frequency));
        }

        // 2. Popular query prefix matching
        Map<String, Double> popularQueries = queryTracker.getPopularQueries();
        for (Map.Entry<String, Double> entry : popularQueries.entrySet()) {
            String popQuery = entry.getKey();
            if (popQuery.startsWith(normalized)) {
                suggestions.add(new SuggestionItem(popQuery, "QUERY", entry.getValue().longValue()));
            }
        }

        // Sort by frequency descending, then lexical ascending, then QUERY before TERM
        suggestions.sort((s1, s2) -> {
            int freqCompare = Long.compare(s2.frequency(), s1.frequency());
            if (freqCompare != 0) {
                return freqCompare;
            }
            int lexicalCompare = s1.text().compareTo(s2.text());
            if (lexicalCompare != 0) {
                return lexicalCompare;
            }
            return s1.type().compareTo(s2.type()); // QUERY before TERM alphabetically
        });

        // Limit results
        List<SuggestionItem> finalSuggestions = suggestions.stream()
                .limit(resolvedLimit)
                .collect(Collectors.toList());

        return new AutocompleteResponse(query, finalSuggestions, finalSuggestions.size());
    }

    @Override
    public AutocompleteStatusResponse getStatus() {
        int vocabSize = invertedIndex.getVocabularySize();
        // Since trie is fully synchronized, index size is identical to prefix trie count
        int trieSize = vocabSize; 
        return new AutocompleteStatusResponse(
                properties.isEnabled(),
                vocabSize,
                trieSize,
                totalRequests.get(),
                queryTracker.getPopularQueries().size(),
                queryTracker.getRecentQueries().size()
        );
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        return prefix.replaceAll("[\\p{Punct}\\p{IsPunctuation}\\p{P}]", "").toLowerCase().trim();
    }
}
