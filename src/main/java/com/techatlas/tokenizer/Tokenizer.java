package com.techatlas.tokenizer;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class Tokenizer {

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        
        // Splits on whitespace
        String[] rawTokens = text.split("\\s+");
        List<String> tokens = new ArrayList<>();
        
        for (String raw : rawTokens) {
            // Removes punctuation, preserves alphanumeric words, supports Unicode text
            String cleaned = raw.replaceAll("[\\p{Punct}\\p{IsPunctuation}\\p{P}]", "");
            if (!cleaned.isEmpty()) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }
}
