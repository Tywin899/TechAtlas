package com.techatlas.autocomplete.service;

import com.techatlas.dto.AutocompleteResponse;
import com.techatlas.dto.AutocompleteStatusResponse;

public interface AutocompleteService {
    AutocompleteResponse getSuggestions(String query, Integer limit);
    AutocompleteStatusResponse getStatus();
}
