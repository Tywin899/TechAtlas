package com.techatlas.dto;

import java.time.LocalDateTime;

public record ZeroResultResponse(
    String query,
    long count,
    LocalDateTime lastOccurrence
) {}
