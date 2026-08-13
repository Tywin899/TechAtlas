CREATE TABLE wikipedia_sync_categories (
    id UUID PRIMARY KEY,
    category_name VARCHAR(255) NOT NULL UNIQUE,
    last_synced_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE wikipedia_sync_articles (
    id UUID PRIMARY KEY,
    article_title VARCHAR(255) NOT NULL UNIQUE,
    imported_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL
);

CREATE INDEX idx_wikipedia_sync_categories_name ON wikipedia_sync_categories(category_name);
CREATE INDEX idx_wikipedia_sync_articles_title ON wikipedia_sync_articles(article_title);
