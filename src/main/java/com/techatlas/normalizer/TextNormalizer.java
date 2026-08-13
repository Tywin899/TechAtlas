package com.techatlas.normalizer;

import com.techatlas.config.IndexProperties;
import org.springframework.stereotype.Component;

@Component
public class TextNormalizer {
    private final IndexProperties indexProperties;

    public TextNormalizer(IndexProperties indexProperties) {
        this.indexProperties = indexProperties;
    }

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        
        if (!indexProperties.isNormalization()) {
            return text;
        }
        
        // lowercase conversion
        String result = text.toLowerCase();
        
        // removing punctuation
        result = result.replaceAll("[\\p{Punct}\\p{IsPunctuation}\\p{P}]", "");
        
        // collapsing repeated spaces
        result = result.replaceAll("\\s+", " ");
        
        // trimming whitespace
        result = result.trim();
        
        return result;
    }
}
