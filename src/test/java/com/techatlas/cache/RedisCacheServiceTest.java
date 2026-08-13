package com.techatlas.cache;

import com.techatlas.config.RedisCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCacheProperties properties;
    private RedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        properties = new RedisCacheProperties();
        cacheService = new RedisCacheService(redisTemplate, properties);
    }

    @Test
    void testGetWhenDisabled() {
        properties.setEnabled(false);
        Optional<Object> result = cacheService.get("key");
        assertFalse(result.isPresent());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void testGetWhenEnabledAndHit() {
        properties.setEnabled(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn("value");

        Optional<Object> result = cacheService.get("key");
        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

    @Test
    void testPutWhenEnabled() {
        properties.setEnabled(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.put("key", "value", 10, TimeUnit.SECONDS);

        verify(valueOperations, times(1)).set("key", "value", 10, TimeUnit.SECONDS);
    }

    @Test
    void testEvictWhenEnabled() {
        properties.setEnabled(true);
        when(redisTemplate.delete("key")).thenReturn(true);

        cacheService.evict("key");

        verify(redisTemplate, times(1)).delete("key");
        assertEquals(1, cacheService.getEvictions());
    }
}
