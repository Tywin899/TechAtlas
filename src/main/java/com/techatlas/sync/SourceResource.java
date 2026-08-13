package com.techatlas.sync;

import com.techatlas.entity.SourceType;

public record SourceResource(
    SourceType source,
    String externalId,
    String externalRevision,
    String title,
    String content,
    String url,
    String author,
    String language,
    String category,
    String metadata
) {}
