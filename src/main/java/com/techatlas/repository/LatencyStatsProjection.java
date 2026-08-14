package com.techatlas.repository;

public interface LatencyStatsProjection {
    Long getTotalQueries();
    Double getAverageMs();
    Long getMinMs();
    Long getMaxMs();
}
