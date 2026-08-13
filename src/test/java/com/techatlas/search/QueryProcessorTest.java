package com.techatlas.search;

import com.techatlas.config.IndexProperties;
import com.techatlas.normalizer.TextNormalizer;
import com.techatlas.stemmer.PorterStemmerAdapter;
import com.techatlas.stopwords.StopWordFilter;
import com.techatlas.tokenizer.Tokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryProcessorTest {

    private QueryProcessor queryProcessor;

    @BeforeEach
    public void setUp() {
        IndexProperties properties = new IndexProperties();
        properties.setStopwords(List.of("the", "is", "a", "and"));
        properties.setStemming(true);
        properties.setNormalization(true);

        Tokenizer tokenizer = new Tokenizer();
        TextNormalizer textNormalizer = new TextNormalizer(properties);
        StopWordFilter stopWordFilter = new StopWordFilter(properties);
        PorterStemmerAdapter porterStemmerAdapter = new PorterStemmerAdapter(properties);

        queryProcessor = new QueryProcessor(tokenizer, textNormalizer, stopWordFilter, porterStemmerAdapter);
    }

    @Test
    public void testQueryProcessingSucceeds() {
        String query = "Spring Framework and Java development!!";
        List<String> terms = queryProcessor.process(query);
        assertThat(terms).containsExactly("spring", "framework", "java", "develop");
    }

    @Test
    public void testQueryProcessingWithEmptyOrNull() {
        assertThat(queryProcessor.process(null)).isEmpty();
        assertThat(queryProcessor.process("   ")).isEmpty();
    }
}
