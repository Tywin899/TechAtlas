package com.techatlas.controller;

import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.WikipediaDiscoverRequest;
import com.techatlas.dto.WikipediaDiscoverResponse;
import com.techatlas.dto.WikipediaSyncStatusResponse;
import com.techatlas.fetcher.wikipedia.dto.WikipediaImportRequest;
import com.techatlas.fetcher.wikipedia.dto.WikipediaPageSummary;
import com.techatlas.service.WikipediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wikipedia")
@Tag(name = "Wikipedia API", description = "Endpoints for importing articles from Wikipedia")
public class WikipediaController {

    private final WikipediaService wikipediaService;

    public WikipediaController(WikipediaService wikipediaService) {
        this.wikipediaService = wikipediaService;
    }

    @PostMapping("/import")
    @Operation(summary = "Import a Wikipedia article by title", description = "Fetches the summary from Wikipedia and persists it in TechAtlas database")
    @ApiResponse(responseCode = "201", description = "Wikipedia article imported successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    @ApiResponse(responseCode = "404", description = "Wikipedia page not found")
    @ApiResponse(responseCode = "409", description = "Conflict: duplicate document content hash already exists")
    @ApiResponse(responseCode = "503", description = "Wikipedia service is unavailable or timeout occurred")
    public ResponseEntity<DocumentResponse> importArticle(@Valid @RequestBody WikipediaImportRequest request) {
        DocumentResponse response = wikipediaService.importArticle(request.title());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{title}")
    @Operation(summary = "Fetch a Wikipedia article by title without persisting", description = "Fetches summary from Wikipedia without storing it in TechAtlas database")
    @ApiResponse(responseCode = "200", description = "Article retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Wikipedia page not found")
    @ApiResponse(responseCode = "503", description = "Wikipedia service is unavailable or timeout occurred")
    public ResponseEntity<WikipediaPageSummary> getArticleSummary(@PathVariable String title) {
        WikipediaPageSummary summary = wikipediaService.fetchSummary(title);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/discover")
    @Operation(summary = "Discover Wikipedia articles by category", description = "Discovers and imports articles recursively from a Wikipedia category up to limits")
    @ApiResponse(responseCode = "200", description = "Category discovery executed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public ResponseEntity<WikipediaDiscoverResponse> discover(@Valid @RequestBody WikipediaDiscoverRequest request) {
        WikipediaDiscoverResponse response = wikipediaService.discoverArticles(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sync")
    @Operation(summary = "Get Wikipedia sync status", description = "Retrieves information about already discovered/synced categories and articles")
    @ApiResponse(responseCode = "200", description = "Sync status retrieved successfully")
    public ResponseEntity<WikipediaSyncStatusResponse> getSyncStatus() {
        WikipediaSyncStatusResponse response = wikipediaService.getSyncStatus();
        return ResponseEntity.ok(response);
    }
}
