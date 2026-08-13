package com.techatlas.dto;

import java.util.List;

public record WikipediaSyncStatusResponse(
    long totalSyncedCategories,
    long totalSyncedArticles,
    List<CategorySyncInfo> categories
) {
    public record CategorySyncInfo(
        String categoryName,
        java.time.LocalDateTime lastSyncedAt
    ) {}
}
