package com.techatlas.dto;

import com.techatlas.entity.SourceType;

import java.time.LocalDateTime;

public record SourceHealthItem(
    SourceType source,
    LocalDateTime lastCheckedAt,
    LocalDateTime lastSyncedAt,
    String status,
    long resourcesChecked,
    long resourcesChanged,
    long resourcesUnchanged,
    long failures,
    long durationMs
) {}
