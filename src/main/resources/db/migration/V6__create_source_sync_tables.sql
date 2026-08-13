CREATE TABLE IF NOT EXISTS source_sync (
    id UUID PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    external_revision VARCHAR(255),
    content_hash VARCHAR(64) NOT NULL,
    last_checked_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_synced_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_error TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_source_sync_source_ext_id UNIQUE (source, external_id)
);

CREATE INDEX IF NOT EXISTS idx_source_sync_source ON source_sync(source);
CREATE INDEX IF NOT EXISTS idx_source_sync_status ON source_sync(status);
CREATE INDEX IF NOT EXISTS idx_source_sync_document ON source_sync(document_id);
