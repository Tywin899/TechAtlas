package com.techatlas.dto;

import com.techatlas.entity.SourceType;

public record SourceSyncResponse(
    SourceType source,
    int checked,
    int newResources,
    int changedResources,
    int unchangedResources,
    int skippedResources,
    int failedResources,
    int createdDocuments,
    int updatedDocuments,
    int indexedDocuments
) {}
