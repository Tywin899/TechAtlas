package com.techatlas.cache;

import com.techatlas.config.RedisCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RedisCacheService implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCacheProperties properties;

    private final AtomicLong searchHits = new AtomicLong(0);
    private final AtomicLong searchMisses = new AtomicLong(0);
    private final AtomicLong documentHits = new AtomicLong(0);
    private final AtomicLong documentMisses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate, RedisCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public boolean isAvailable() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            org.springframework.data.redis.connection.RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory == null) {
                return false;
            }
            String response = factory.getConnection().ping();
            return "PONG".equalsIgnoreCase(response);
        } catch (Exception e) {
            logger.debug("Redis availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Object> get(String key) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            Object val = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(val);
        } catch (Exception e) {
            logger.error("Redis error during GET for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, long ttl, TimeUnit unit) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttl, unit);
        } catch (Exception e) {
            logger.error("Redis error during PUT for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void evict(String key) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                incrementEvictions();
            }
        } catch (Exception e) {
            logger.error("Redis error during EVICT for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void clearPattern(String pattern) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long count = redisTemplate.delete(keys);
                if (count != null && count > 0) {
                    evictions.addAndGet(count);
                }
            }
        } catch (Exception e) {
            logger.error("Redis error during clearPattern for pattern {}: {}", pattern, e.getMessage());
        }
    }

    @Override
    public void clearAllSearchCaches() {
        clearPattern("search:*");
    }

    @Override
    public long getSearchHits() {
        return searchHits.get();
    }

    @Override
    public long getSearchMisses() {
        return searchMisses.get();
    }

    @Override
    public long getDocumentHits() {
        return documentHits.get();
    }

    @Override
    public long getDocumentMisses() {
        return documentMisses.get();
    }

    @Override
    public long getEvictions() {
        return evictions.get();
    }

    @Override
    public void incrementSearchHits() {
        searchHits.incrementAndGet();
    }

    @Override
    public void incrementSearchMisses() {
        searchMisses.incrementAndGet();
    }

    @Override
    public void incrementDocumentHits() {
        documentHits.incrementAndGet();
    }

    @Override
    public void incrementDocumentMisses() {
        documentMisses.incrementAndGet();
    }

    @Override
    public void incrementEvictions() {
        evictions.incrementAndGet();
    }
}
