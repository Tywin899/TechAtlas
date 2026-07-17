CREATE TABLE IF NOT EXISTS document (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    url VARCHAR(2048) NOT NULL,
    source VARCHAR(100) NOT NULL,
    metadata JSONB,
    content_hash VARCHAR(256) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS search_query (
    id BIGSERIAL PRIMARY KEY,
    query VARCHAR(500) NOT NULL,
    result_count INTEGER NOT NULL,
    searched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analytics_daily (
    id BIGSERIAL PRIMARY KEY,
    day DATE NOT NULL UNIQUE,
    total_queries INTEGER NOT NULL DEFAULT 0,
    indexed_documents INTEGER NOT NULL DEFAULT 0
);

-- Database indexes for optimized lookup performance
CREATE INDEX IF NOT EXISTS idx_document_source ON document(source);
CREATE UNIQUE INDEX IF NOT EXISTS idx_document_url ON document(url);
CREATE INDEX IF NOT EXISTS idx_document_content_hash ON document(content_hash);
CREATE INDEX IF NOT EXISTS idx_search_query_searched_at ON search_query(searched_at);
CREATE INDEX IF NOT EXISTS idx_analytics_daily_day ON analytics_daily(day);
