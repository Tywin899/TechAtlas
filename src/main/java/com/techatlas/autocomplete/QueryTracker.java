package com.techatlas.autocomplete;

import com.techatlas.cache.CacheService;
import com.techatlas.config.AutocompleteProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QueryTracker {

    private static final Logger logger = LoggerFactory.getLogger(QueryTracker.class);
    private static final String POPULAR_KEY = "autocomplete:popular";
    private static final String RECENT_KEY = "autocomplete:recent";

    private final CacheService cacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AutocompleteProperties properties;

    // Fallbacks when Redis is not available
    private final Map<String, Double> fallbackPopular = new ConcurrentHashMap<>();
    private final List<String> fallbackRecent = Collections.synchronizedList(new LinkedList<>());

    public QueryTracker(CacheService cacheService,
                        RedisTemplate<String, Object> redisTemplate,
                        AutocompleteProperties properties) {
        this.cacheService = cacheService;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void trackQuery(String query) {
        if (!properties.isEnabled() || query == null || query.isBlank()) {
            return;
        }

        String normalized = query.trim().toLowerCase();
        if (normalized.length() > properties.getMaxPrefixLength()) {
            normalized = normalized.substring(0, properties.getMaxPrefixLength());
        }

        if (cacheService.isAvailable()) {
            try {
                if (properties.getPopularQuery().isEnabled()) {
                    redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, normalized, 1.0);
                }
                if (properties.getRecentQuery().isEnabled()) {
                    redisTemplate.opsForList().remove(RECENT_KEY, 0, normalized);
                    redisTemplate.opsForList().leftPush(RECENT_KEY, normalized);
                    redisTemplate.opsForList().trim(RECENT_KEY, 0, properties.getRecentQuery().getMaxSize() - 1);
                }
                return;
            } catch (Exception e) {
                logger.warn("Failed to write query metrics to Redis, falling back to in-memory: {}", e.getMessage());
            }
        }

        // Fallback execution
        if (properties.getPopularQuery().isEnabled()) {
            fallbackPopular.merge(normalized, 1.0, Double::sum);
        }
        if (properties.getRecentQuery().isEnabled()) {
            synchronized (fallbackRecent) {
                fallbackRecent.remove(normalized);
                fallbackRecent.add(0, normalized);
                if (fallbackRecent.size() > properties.getRecentQuery().getMaxSize()) {
                    fallbackRecent.remove(fallbackRecent.size() - 1);
                }
            }
        }
    }

    public Map<String, Double> getPopularQueries() {
        if (!properties.isEnabled() || !properties.getPopularQuery().isEnabled()) {
            return Collections.emptyMap();
        }

        if (cacheService.isAvailable()) {
            try {
                Set<TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                        .reverseRangeWithScores(POPULAR_KEY, 0, properties.getPopularQuery().getMaxSize() - 1);
                if (tuples != null && !tuples.isEmpty()) {
                    Map<String, Double> results = new LinkedHashMap<>();
                    for (TypedTuple<Object> tuple : tuples) {
                        if (tuple.getValue() != null) {
                            results.put(tuple.getValue().toString(), tuple.getScore());
                        }
                    }
                    return results;
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch popular queries from Redis: {}", e.getMessage());
            }
        }

        // Return sorted local fallback map
        List<Map.Entry<String, Double>> list = new ArrayList<>(fallbackPopular.entrySet());
        list.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        Map<String, Double> sortedFallback = new LinkedHashMap<>();
        int limit = Math.min(list.size(), properties.getPopularQuery().getMaxSize());
        for (int i = 0; i < limit; i++) {
            sortedFallback.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return sortedFallback;
    }

    public List<String> getRecentQueries() {
        if (!properties.isEnabled() || !properties.getRecentQuery().isEnabled()) {
            return Collections.emptyList();
        }

        if (cacheService.isAvailable()) {
            try {
                List<Object> range = redisTemplate.opsForList()
                        .range(RECENT_KEY, 0, properties.getRecentQuery().getMaxSize() - 1);
                if (range != null) {
                    List<String> results = new ArrayList<>();
                    for (Object obj : range) {
                        if (obj != null) {
                            results.add(obj.toString());
                        }
                    }
                    return results;
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch recent queries from Redis: {}", e.getMessage());
            }
        }

        synchronized (fallbackRecent) {
            return new ArrayList<>(fallbackRecent);
        }
    }

    public void clearFallbackData() {
        fallbackPopular.clear();
        fallbackRecent.clear();
    }
}
