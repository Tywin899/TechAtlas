package com.techatlas.repository;

import com.techatlas.entity.SearchAnalytics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchAnalyticsRepository extends JpaRepository<SearchAnalytics, UUID> {

    @Query("SELECT s.normalizedQuery as query, COUNT(s) as count " +
           "FROM SearchAnalytics s " +
           "GROUP BY s.normalizedQuery " +
           "ORDER BY count DESC")
    List<QueryCountProjection> findTopQueries(Pageable pageable);

    @Query("SELECT s.normalizedQuery as query, COUNT(s) as count, MAX(s.timestamp) as lastOccurrence " +
           "FROM SearchAnalytics s " +
           "WHERE s.zeroResults = true " +
           "GROUP BY s.normalizedQuery " +
           "ORDER BY count DESC")
    List<ZeroResultProjection> findZeroResultQueries(Pageable pageable);

    @Query("SELECT COUNT(s) as totalQueries, " +
           "COALESCE(AVG(s.latencyMs), 0.0) as averageMs, " +
           "COALESCE(MIN(s.latencyMs), 0L) as minMs, " +
           "COALESCE(MAX(s.latencyMs), 0L) as maxMs " +
           "FROM SearchAnalytics s")
    LatencyStatsProjection findLatencyStats();

    @Query("SELECT s.latencyMs FROM SearchAnalytics s ORDER BY s.timestamp DESC")
    List<Long> findRecentLatencies(Pageable pageable);

    long countByZeroResults(boolean zeroResults);
}
