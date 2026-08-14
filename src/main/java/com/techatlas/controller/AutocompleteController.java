package com.techatlas.controller;

import com.techatlas.autocomplete.service.AutocompleteService;
import com.techatlas.dto.AutocompleteResponse;
import com.techatlas.dto.AutocompleteStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Autocomplete & Suggestions", description = "Endpoints for prefix term matching and popular search suggestions")
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    @GetMapping("/search/suggestions")
    @Operation(summary = "Get prefix suggestions and popular queries")
    public ResponseEntity<AutocompleteResponse> getSuggestions(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "limit", required = false) Integer limit) {
        AutocompleteResponse response = autocompleteService.getSuggestions(query, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete/status")
    @Operation(summary = "Get status and diagnostics of the autocomplete index")
    public ResponseEntity<AutocompleteStatusResponse> getStatus() {
        AutocompleteStatusResponse status = autocompleteService.getStatus();
        return ResponseEntity.ok(status);
    }
}
