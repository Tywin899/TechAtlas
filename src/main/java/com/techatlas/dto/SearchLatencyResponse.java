package com.techatlas.dto;

public record SearchLatencyResponse(
    long totalQueries,
    double averageMs,
    long minMs,
    long maxMs,
    double p50Ms,
    double p90Ms,
    double p95Ms,
    double p99Ms
) {}
