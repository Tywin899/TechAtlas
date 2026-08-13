package com.techatlas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stackoverflow_sync_questions")
public class StackOverflowSyncQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false, unique = true)
    private Long questionId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Column(name = "document_id")
    private UUID documentId;

    public StackOverflowSyncQuestion() {}

    public StackOverflowSyncQuestion(Long questionId, String title, LocalDateTime lastSyncedAt, UUID documentId) {
        this.questionId = questionId;
        this.title = title;
        this.lastSyncedAt = lastSyncedAt;
        this.documentId = documentId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }
}
