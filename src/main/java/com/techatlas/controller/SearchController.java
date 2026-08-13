package com.techatlas.controller;

import com.techatlas.dto.SearchRequest;
import com.techatlas.dto.SearchResponse;
import com.techatlas.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search API", description = "Endpoints for searching indexed documents")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Search documents", description = "Retrieves and ranks indexed documents matching search terms using BM25")
    @ApiResponse(responseCode = "200", description = "Search query processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid query or pagination parameters")
    public ResponseEntity<SearchResponse> search(
            @RequestParam("q") String query,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        
        SearchRequest request = new SearchRequest(query, page, size);
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }
}
