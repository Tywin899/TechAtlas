package com.techatlas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wikipedia_sync_articles")
public class WikipediaSyncArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "article_title", nullable = false, unique = true)
    private String articleTitle;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "document_id")
    private UUID documentId;

    public WikipediaSyncArticle() {}

    public WikipediaSyncArticle(String articleTitle, LocalDateTime importedAt, UUID documentId) {
        this.articleTitle = articleTitle;
        this.importedAt = importedAt;
        this.documentId = documentId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }
}
