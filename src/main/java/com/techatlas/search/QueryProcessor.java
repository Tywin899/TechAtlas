package com.techatlas.search;

import com.techatlas.normalizer.TextNormalizer;
import com.techatlas.stemmer.PorterStemmerAdapter;
import com.techatlas.stopwords.StopWordFilter;
import com.techatlas.tokenizer.Tokenizer;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class QueryProcessor {
    private final Tokenizer tokenizer;
    private final TextNormalizer textNormalizer;
    private final StopWordFilter stopWordFilter;
    private final PorterStemmerAdapter porterStemmerAdapter;

    public QueryProcessor(
            Tokenizer tokenizer,
            TextNormalizer textNormalizer,
            StopWordFilter stopWordFilter,
            PorterStemmerAdapter porterStemmerAdapter) {
        this.tokenizer = tokenizer;
        this.textNormalizer = textNormalizer;
        this.stopWordFilter = stopWordFilter;
        this.porterStemmerAdapter = porterStemmerAdapter;
    }

    public List<String> process(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<String> tokens = tokenizer.tokenize(query);
        List<String> terms = new ArrayList<>();

        for (String token : tokens) {
            String normalized = textNormalizer.normalize(token);
            if (normalized.isEmpty()) {
                continue;
            }
            if (stopWordFilter.isStopWord(normalized)) {
                continue;
            }
            String stemmed = porterStemmerAdapter.stem(normalized);
            if (stemmed.isEmpty()) {
                continue;
            }
            terms.add(stemmed);
        }

        return terms.stream().distinct().toList();
    }
}
