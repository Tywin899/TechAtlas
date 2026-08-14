package com.techatlas.dto;

import com.techatlas.entity.SourceType;

import java.util.Map;
import java.util.Set;

public record SchedulerStatusResponse(
    boolean enabled,
    Set<SourceType> runningSources,
    Map<SourceType, Boolean> configuredSources,
    long intervalMs,
    long initialDelayMs
) {}
