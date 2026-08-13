package com.techatlas.dto;

import java.util.List;

public record StackOverflowSyncStatusResponse(
    long totalSyncedQuestions,
    List<QuestionSyncInfo> questions
) {
    public record QuestionSyncInfo(
        Long questionId,
        String title,
        java.time.LocalDateTime lastSyncedAt
    ) {}
}
