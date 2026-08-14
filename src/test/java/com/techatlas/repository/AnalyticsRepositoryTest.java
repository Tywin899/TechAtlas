package com.techatlas.repository;

import com.techatlas.entity.SearchAnalytics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class AnalyticsRepositoryTest {

    @Autowired
    private SearchAnalyticsRepository repository;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();

        // 3 searches for "java" (2 zero-result)
        repository.save(new SearchAnalytics(UUID.randomUUID(), "Java", "java", LocalDateTime.now().minusMinutes(5), 10L, 0, 10, 20L, false, false));
        repository.save(new SearchAnalytics(UUID.randomUUID(), "java", "java", LocalDateTime.now().minusMinutes(4), 0L, 0, 10, 10L, true, false));
        repository.save(new SearchAnalytics(UUID.randomUUID(), "JAVA", "java", LocalDateTime.now().minusMinutes(3), 0L, 1, 10, 15L, true, true));

        // 1 search for "spring" (0 zero-result)
        repository.save(new SearchAnalytics(UUID.randomUUID(), "spring", "spring", LocalDateTime.now().minusMinutes(2), 5L, 0, 10, 35L, false, false));
    }

    @Test
    public void testFindTopQueriesOrderedByCount() {
        List<QueryCountProjection> results = repository.findTopQueries(PageRequest.of(0, 10));
        assertThat(results).hasSize(2);
        
        assertEquals("java", results.get(0).getQuery());
        assertEquals(3L, results.get(0).getCount());

        assertEquals("spring", results.get(1).getQuery());
        assertEquals(1L, results.get(1).getCount());
    }

    @Test
    public void testFindZeroResultQueries() {
        List<ZeroResultProjection> results = repository.findZeroResultQueries(PageRequest.of(0, 10));
        assertThat(results).hasSize(1);
        
        assertEquals("java", results.get(0).getQuery());
        assertEquals(2L, results.get(0).getCount());
        assertThat(results.get(0).getLastOccurrence()).isNotNull();
    }

    @Test
    public void testFindLatencyStats() {
        LatencyStatsProjection stats = repository.findLatencyStats();
        assertThat(stats).isNotNull();
        assertEquals(4L, stats.getTotalQueries());
        assertEquals((20.0 + 10.0 + 15.0 + 35.0) / 4.0, stats.getAverageMs(), 0.001);
        assertEquals(10L, stats.getMinMs());
        assertEquals(35L, stats.getMaxMs());
    }
}
