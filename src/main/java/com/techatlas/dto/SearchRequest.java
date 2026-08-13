package com.techatlas.dto;

public record SearchRequest(
    String query,
    Integer page,
    Integer size
) {}
