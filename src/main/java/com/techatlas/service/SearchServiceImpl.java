package com.techatlas.service;

import com.techatlas.config.SearchProperties;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.SearchRequest;
import com.techatlas.dto.SearchResult;
import com.techatlas.dto.SearchResponse;
import com.techatlas.search.QueryProcessor;
import com.techatlas.search.RankingEngine;
import com.techatlas.stemmer.PorterStemmerAdapter;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

import com.techatlas.cache.CacheService;
import com.techatlas.config.RedisCacheProperties;
import com.techatlas.util.HashUtil;
import java.util.concurrent.TimeUnit;

@Service
public class SearchServiceImpl implements SearchService {

    private final DocumentService documentService;
    private final QueryProcessor queryProcessor;
    private final RankingEngine rankingEngine;
    private final PorterStemmerAdapter porterStemmerAdapter;
    private final SearchProperties searchProperties;
    private final CacheService cacheService;
    private final RedisCacheProperties redisCacheProperties;

    public SearchServiceImpl(
            DocumentService documentService,
            QueryProcessor queryProcessor,
            RankingEngine rankingEngine,
            PorterStemmerAdapter porterStemmerAdapter,
            SearchProperties searchProperties,
            CacheService cacheService,
            RedisCacheProperties redisCacheProperties) {
        this.documentService = documentService;
        this.queryProcessor = queryProcessor;
        this.rankingEngine = rankingEngine;
        this.porterStemmerAdapter = porterStemmerAdapter;
        this.searchProperties = searchProperties;
        this.cacheService = cacheService;
        this.redisCacheProperties = redisCacheProperties;
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        String query = request.query();
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query parameter 'q' must not be empty");
        }

        int defaultSize = searchProperties.getPagination().getDefaultSize();
        int maxSize = searchProperties.getPagination().getMaxSize();

        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : defaultSize;

        if (page < 0) {
            throw new IllegalArgumentException("Page index must be non-negative");
        }
        if (size <= 0 || size > maxSize) {
            throw new IllegalArgumentException("Page size must be positive and not exceed " + maxSize);
        }

        String canonicalRequest = query.trim().toLowerCase() + "|" + page + "|" + size;
        String keyHash = HashUtil.calculateSha256(canonicalRequest);
        String cacheKey = "search:" + keyHash;

        Optional<Object> cached = cacheService.get(cacheKey);
        if (cached.isPresent()) {
            cacheService.incrementSearchHits();
            return (SearchResponse) cached.get();
        }
        cacheService.incrementSearchMisses();

        List<String> queryTerms = queryProcessor.process(query);
        if (queryTerms.isEmpty()) {
            SearchResponse emptyResponse = new SearchResponse(query, 0, Collections.emptyList(), page, size, 0);
            cacheService.put(cacheKey, emptyResponse, redisCacheProperties.getSearch().getTtlSeconds(), TimeUnit.SECONDS);
            return emptyResponse;
        }

        Map<UUID, Double> scores = rankingEngine.scoreDocuments(queryTerms);
        if (scores.isEmpty()) {
            return new SearchResponse(query, 0, Collections.emptyList(), page, size, 0);
        }

        List<Map.Entry<UUID, Double>> sortedEntries = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        long totalResults = sortedEntries.size();
        int totalPages = (int) Math.ceil((double) totalResults / size);

        int startOffset = page * size;
        SearchResponse response;
        if (startOffset >= totalResults) {
            response = new SearchResponse(query, totalResults, Collections.emptyList(), page, size, totalPages);
        } else {
            int endOffset = Math.min(sortedEntries.size(), startOffset + size);
            List<Map.Entry<UUID, Double>> pageEntries = sortedEntries.subList(startOffset, endOffset);

            List<SearchResult> results = new ArrayList<>();
            for (Map.Entry<UUID, Double> entry : pageEntries) {
                UUID docId = entry.getKey();
                double score = entry.getValue();

                DocumentResponse doc = documentService.retrieve(docId);
                String snippet = generateSnippet(doc.content(), queryTerms);

                double roundedScore = Math.round(score * 100.0) / 100.0;

                results.add(new SearchResult(
                        doc.id(),
                        doc.title(),
                        doc.source(),
                        doc.url(),
                        roundedScore,
                        snippet,
                        doc.indexedAt()
                ));
            }

            response = new SearchResponse(query, totalResults, results, page, size, totalPages);
        }

        cacheService.put(cacheKey, response, redisCacheProperties.getSearch().getTtlSeconds(), TimeUnit.SECONDS);
        return response;
    }

    private String generateSnippet(String content, List<String> stemmedQueryTerms) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        if (stemmedQueryTerms == null || stemmedQueryTerms.isEmpty()) {
            return content.substring(0, Math.min(content.length(), 150)) + (content.length() > 150 ? "..." : "");
        }

        String[] words = content.split("\\s+");
        int matchIndex = -1;

        for (String word : words) {
            String cleaned = word.replaceAll("[\\p{Punct}\\p{IsPunctuation}\\p{P}]", "").toLowerCase().trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            String stemmed = porterStemmerAdapter.stem(cleaned);
            if (stemmedQueryTerms.contains(stemmed)) {
                matchIndex = content.indexOf(word);
                if (matchIndex != -1) {
                    break;
                }
            }
        }

        if (matchIndex == -1) {
            return content.substring(0, Math.min(content.length(), 150)) + (content.length() > 150 ? "..." : "");
        }

        int start = Math.max(0, matchIndex - 60);
        int end = Math.min(content.length(), matchIndex + 90);

        String snippet = content.substring(start, end);
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < content.length()) {
            snippet = snippet + "...";
        }
        return snippet;
    }
}
