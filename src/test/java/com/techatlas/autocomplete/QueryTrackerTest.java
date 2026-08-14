package com.techatlas.autocomplete;

import com.techatlas.cache.CacheService;
import com.techatlas.config.AutocompleteProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryTrackerTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    private AutocompleteProperties properties;
    private QueryTracker queryTracker;

    @BeforeEach
    public void setUp() {
        properties = new AutocompleteProperties();
        properties.getPopularQuery().setEnabled(true);
        properties.getPopularQuery().setMaxSize(5);
        properties.getRecentQuery().setEnabled(true);
        properties.getRecentQuery().setMaxSize(5);

        queryTracker = new QueryTracker(cacheService, redisTemplate, properties);
    }

    @Test
    public void testTrackQueryFallbackInMemory() {
        when(cacheService.isAvailable()).thenReturn(false);

        queryTracker.trackQuery("Spring Boot");
        queryTracker.trackQuery("spring boot "); // duplicate checks
        queryTracker.trackQuery("Java");

        Map<String, Double> popular = queryTracker.getPopularQueries();
        assertThat(popular).containsEntry("spring boot", 2.0);
        assertThat(popular).containsEntry("java", 1.0);

        List<String> recent = queryTracker.getRecentQueries();
        assertThat(recent).containsExactly("java", "spring boot");
    }

    @Test
    public void testTrackQueryRedisActive() {
        when(cacheService.isAvailable()).thenReturn(true);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        queryTracker.trackQuery("Spring");

        verify(zSetOperations, times(1)).incrementScore(eq("autocomplete:popular"), eq("spring"), eq(1.0));
        verify(listOperations, times(1)).remove(eq("autocomplete:recent"), eq(0L), eq("spring"));
        verify(listOperations, times(1)).leftPush(eq("autocomplete:recent"), eq("spring"));
        verify(listOperations, times(1)).trim(eq("autocomplete:recent"), eq(0L), eq(4L));
    }

    @Test
    public void testTrackQueryRedisFailureGracefulFallback() {
        when(cacheService.isAvailable()).thenReturn(true);
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("Redis connection lost"));

        // Should fall back silently without throwing exception
        queryTracker.trackQuery("Spring Fallback");

        // Verify fallback tracking worked
        Map<String, Double> popular = queryTracker.getPopularQueries();
        assertThat(popular).containsEntry("spring fallback", 1.0);
    }
}
