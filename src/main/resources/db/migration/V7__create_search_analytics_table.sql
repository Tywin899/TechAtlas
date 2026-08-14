CREATE TABLE IF NOT EXISTS search_analytics (
    id UUID PRIMARY KEY,
    query VARCHAR(500) NOT NULL,
    normalized_query VARCHAR(500) NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    result_count BIGINT NOT NULL,
    requested_page INTEGER NOT NULL,
    requested_size INTEGER NOT NULL,
    latency_ms BIGINT NOT NULL,
    zero_results BOOLEAN NOT NULL,
    served_from_cache BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_search_analytics_normalized_query ON search_analytics(normalized_query);
CREATE INDEX IF NOT EXISTS idx_search_analytics_timestamp ON search_analytics(timestamp);
CREATE INDEX IF NOT EXISTS idx_search_analytics_zero_results ON search_analytics(zero_results);
