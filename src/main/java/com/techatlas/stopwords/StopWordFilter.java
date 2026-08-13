package com.techatlas.stopwords;

import com.techatlas.config.IndexProperties;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StopWordFilter {
    private final Set<String> stopWords;

    public StopWordFilter(IndexProperties indexProperties) {
        if (indexProperties != null && indexProperties.getStopwords() != null) {
            this.stopWords = indexProperties.getStopwords().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        } else {
            this.stopWords = Set.of(
                "a", "an", "the", "of", "is", "are", "was", "were", 
                "to", "for", "and", "or", "with"
            );
        }
    }

    public boolean isStopWord(String word) {
        if (word == null) {
            return false;
        }
        return stopWords.contains(word.toLowerCase().trim());
    }

    public Set<String> getStopWords() {
        return Collections.unmodifiableSet(stopWords);
    }
}
