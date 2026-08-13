package com.techatlas.dto;

public record CacheStatusResponse(
    boolean enabled,
    boolean available,
    long searchHits,
    long searchMisses,
    long documentHits,
    long documentMisses,
    long evictions
) {}
