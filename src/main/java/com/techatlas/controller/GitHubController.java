package com.techatlas.controller;

import com.techatlas.dto.GitHubDiscoverRequest;
import com.techatlas.dto.GitHubDiscoverResponse;
import com.techatlas.dto.GitHubSyncStatusResponse;
import com.techatlas.service.GitHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/github")
@Tag(name = "GitHub API", description = "Endpoints for discovering repositories from GitHub")
public class GitHubController {

    private final GitHubService githubService;

    public GitHubController(GitHubService githubService) {
        this.githubService = githubService;
    }

    @PostMapping("/discover")
    @Operation(summary = "Discover repositories from GitHub", description = "Searches for repositories on GitHub and imports them into the system")
    @ApiResponse(responseCode = "200", description = "GitHub discovery executed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public ResponseEntity<GitHubDiscoverResponse> discover(@Valid @RequestBody GitHubDiscoverRequest request) {
        GitHubDiscoverResponse response = githubService.discoverRepositories(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sync")
    @Operation(summary = "Get GitHub sync status", description = "Retrieves information about already discovered/synced GitHub repositories")
    @ApiResponse(responseCode = "200", description = "Sync status retrieved successfully")
    public ResponseEntity<GitHubSyncStatusResponse> getSyncStatus() {
        GitHubSyncStatusResponse response = githubService.getSyncStatus();
        return ResponseEntity.ok(response);
    }
}
