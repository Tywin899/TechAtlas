package com.techatlas.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wikipedia_sync_categories")
public class WikipediaSyncCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    public WikipediaSyncCategory() {}

    public WikipediaSyncCategory(String categoryName, LocalDateTime lastSyncedAt) {
        this.categoryName = categoryName;
        this.lastSyncedAt = lastSyncedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
