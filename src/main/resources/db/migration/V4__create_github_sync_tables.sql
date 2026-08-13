CREATE TABLE IF NOT EXISTS github_sync_repositories (
    id UUID PRIMARY KEY,
    github_repo_id BIGINT UNIQUE NOT NULL,
    repository_name VARCHAR(255) NOT NULL,
    last_synced_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_github_sync_repositories_repo_id ON github_sync_repositories(github_repo_id);
