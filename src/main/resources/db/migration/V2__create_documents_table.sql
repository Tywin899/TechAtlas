CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    url VARCHAR(2048) NOT NULL,
    source VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    author VARCHAR(255),
    language VARCHAR(50),
    content_hash VARCHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    indexed_at TIMESTAMP WITHOUT TIME ZONE,
    metadata TEXT,
    CONSTRAINT uq_documents_content_hash UNIQUE (content_hash)
);

CREATE INDEX IF NOT EXISTS idx_documents_source ON documents(source);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_category ON documents(category);
CREATE INDEX IF NOT EXISTS idx_documents_content_hash ON documents(content_hash);
