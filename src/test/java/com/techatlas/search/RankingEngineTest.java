package com.techatlas.search;

import com.techatlas.config.SearchProperties;
import com.techatlas.model.InvertedIndex;
import com.techatlas.model.Posting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

public class RankingEngineTest {

    private InvertedIndex invertedIndex;
    private SearchProperties searchProperties;
    private RankingEngine rankingEngine;

    @BeforeEach
    public void setUp() {
        invertedIndex = new InvertedIndex();
        searchProperties = new SearchProperties();
        searchProperties.getBm25().setK1(1.2);
        searchProperties.getBm25().setB(0.75);

        rankingEngine = new RankingEngine(searchProperties, invertedIndex);
    }

    @Test
    public void testScoreDocuments() {
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();

        invertedIndex.setDocumentLength(docA, 10);
        invertedIndex.setDocumentLength(docB, 20);

        invertedIndex.insert("java", new Posting(docA, 3));
        invertedIndex.insert("java", new Posting(docB, 1));

        Map<UUID, Double> scores = rankingEngine.scoreDocuments(List.of("java"));

        assertThat(scores).hasSize(2);
        assertThat(scores.get(docA)).isGreaterThan(scores.get(docB));
    }
}
