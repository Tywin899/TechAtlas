package com.techatlas.dto;

import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;

import java.util.Map;

public record DocumentStatsResponse(
    long totalDocuments,
    Map<SourceType, Long> bySource,
    Map<DocumentStatus, Long> byStatus,
    long indexedDocuments,
    long pendingDocuments,
    long failedDocuments,
    Map<String, Long> byCategory
) {}
