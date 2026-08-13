package com.techatlas.controller;

import com.techatlas.cache.CacheService;
import com.techatlas.dto.CacheStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cache")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/status")
    public ResponseEntity<CacheStatusResponse> getCacheStatus() {
        CacheStatusResponse response = new CacheStatusResponse(
                cacheService.isEnabled(),
                cacheService.isAvailable(),
                cacheService.getSearchHits(),
                cacheService.getSearchMisses(),
                cacheService.getDocumentHits(),
                cacheService.getDocumentMisses(),
                cacheService.getEvictions()
        );
        return ResponseEntity.ok(response);
    }
}
