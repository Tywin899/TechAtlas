package com.techatlas.dto;

import java.util.List;

public record SyncHealthResponse(
    List<SourceHealthItem> sources
) {}
