package com.techatlas.stemmer;

import com.techatlas.config.IndexProperties;
import org.springframework.stereotype.Component;

@Component
public class PorterStemmerAdapter {
    private final IndexProperties indexProperties;

    public PorterStemmerAdapter(IndexProperties indexProperties) {
        this.indexProperties = indexProperties;
    }

    public String stem(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        
        if (!indexProperties.isStemming()) {
            return word;
        }

        Stemmer stemmer = new Stemmer();
        stemmer.add(word.toCharArray(), word.length());
        stemmer.stem();
        return stemmer.toString();
    }
}
