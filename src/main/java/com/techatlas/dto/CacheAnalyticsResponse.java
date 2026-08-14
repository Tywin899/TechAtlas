package com.techatlas.dto;

public record CacheAnalyticsResponse(
    long hits,
    long misses,
    long evictions,
    double hitRatio,
    boolean available
) {}
