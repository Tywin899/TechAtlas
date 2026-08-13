CREATE TABLE IF NOT EXISTS stackoverflow_sync_questions (
    id UUID PRIMARY KEY,
    question_id BIGINT UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    last_synced_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_stackoverflow_sync_questions_question_id ON stackoverflow_sync_questions(question_id);
