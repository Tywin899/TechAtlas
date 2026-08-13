package com.techatlas.dto;

import com.techatlas.entity.SourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record SearchResult(
    UUID id,
    String title,
    SourceType source,
    String url,
    double score,
    String snippet,
    LocalDateTime indexedAt
) {}
