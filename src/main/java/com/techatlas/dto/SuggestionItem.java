package com.techatlas.dto;

public record SuggestionItem(
    String text,
    String type, // "TERM" or "QUERY" or "RECENT"
    long frequency
) {}
