package com.techatlas.dto;

import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    String title,
    String content,
    String url,
    SourceType source,
    String category,
    String author,
    String language,
    String contentHash,
    DocumentStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime indexedAt,
    String metadata
) {}
