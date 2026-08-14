package com.techatlas.dto;

public record TopQueryResponse(
    String query,
    long count
) {}
