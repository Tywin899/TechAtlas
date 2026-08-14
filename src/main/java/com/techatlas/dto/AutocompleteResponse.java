package com.techatlas.dto;

import java.util.List;

public record AutocompleteResponse(
    String query,
    List<SuggestionItem> suggestions,
    int count
) {}
