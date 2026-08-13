package com.techatlas.search;

import com.techatlas.config.SearchProperties;
import com.techatlas.model.InvertedIndex;
import com.techatlas.model.Posting;
import com.techatlas.model.PostingList;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RankingEngine {
    private final SearchProperties searchProperties;
    private final InvertedIndex invertedIndex;

    public RankingEngine(SearchProperties searchProperties, InvertedIndex invertedIndex) {
        this.searchProperties = searchProperties;
        this.invertedIndex = invertedIndex;
    }

    public Map<UUID, Double> scoreDocuments(List<String> queryTerms) {
        Map<UUID, Double> scores = new HashMap<>();

        int N = invertedIndex.getDocumentCount();
        if (N == 0 || queryTerms.isEmpty()) {
            return scores;
        }

        double avgdl = invertedIndex.getAverageDocumentLength();
        double k1 = searchProperties.getBm25().getK1();
        double b = searchProperties.getBm25().getB();

        for (String term : queryTerms) {
            PostingList postingList = invertedIndex.retrieve(term);
            if (postingList == null) {
                continue;
            }

            List<Posting> postings = postingList.getPostings();
            int df = postings.size();
            if (df == 0) {
                continue;
            }

            double idf = Math.log(1.0 + (double) (N - df + 0.5) / (df + 0.5));

            for (Posting posting : postings) {
                UUID docId = posting.documentId();
                int tf = posting.termFrequency();
                int docLen = invertedIndex.getDocumentLength(docId);

                double lenRatio = avgdl > 0 ? (docLen / avgdl) : 1.0;

                double numerator = tf * (k1 + 1.0);
                double denominator = tf + k1 * (1.0 - b + b * lenRatio);
                double termScore = idf * (numerator / denominator);

                scores.put(docId, scores.getOrDefault(docId, 0.0) + termScore);
            }
        }

        return scores;
    }
}
