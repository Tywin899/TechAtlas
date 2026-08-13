package com.techatlas.controller;

import com.techatlas.dto.StackOverflowDiscoverRequest;
import com.techatlas.dto.StackOverflowDiscoverResponse;
import com.techatlas.dto.StackOverflowSyncStatusResponse;
import com.techatlas.service.StackOverflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stackoverflow")
@Tag(name = "Stack Overflow API", description = "Endpoints for discovering questions from Stack Overflow")
public class StackOverflowController {

    private final StackOverflowService stackOverflowService;

    public StackOverflowController(StackOverflowService stackOverflowService) {
        this.stackOverflowService = stackOverflowService;
    }

    @PostMapping("/discover")
    @Operation(summary = "Discover questions from Stack Overflow", description = "Searches for questions on Stack Overflow and imports them into the system")
    @ApiResponse(responseCode = "200", description = "Stack Overflow discovery executed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public ResponseEntity<StackOverflowDiscoverResponse> discover(@Valid @RequestBody StackOverflowDiscoverRequest request) {
        StackOverflowDiscoverResponse response = stackOverflowService.discoverQuestions(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sync")
    @Operation(summary = "Get Stack Overflow sync status", description = "Retrieves information about already discovered/synced Stack Overflow questions")
    @ApiResponse(responseCode = "200", description = "Sync status retrieved successfully")
    public ResponseEntity<StackOverflowSyncStatusResponse> getSyncStatus() {
        StackOverflowSyncStatusResponse response = stackOverflowService.getSyncStatus();
        return ResponseEntity.ok(response);
    }
}
