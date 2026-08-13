package com.techatlas.controller;

import com.techatlas.dto.IndexStatusResponse;
import com.techatlas.index.IndexService;
import com.techatlas.model.InvertedIndex;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/index")
@Tag(name = "Index API", description = "Endpoints for managing the search inverted index")
public class IndexController {

    private final IndexService indexService;
    private final InvertedIndex invertedIndex;

    public IndexController(IndexService indexService, InvertedIndex invertedIndex) {
        this.indexService = indexService;
        this.invertedIndex = invertedIndex;
    }

    @PostMapping("/rebuild")
    @Operation(summary = "Rebuild index", description = "Clears and rebuilds the inverted index by processing all non-deleted documents")
    @ApiResponse(responseCode = "200", description = "Index rebuilt successfully")
    public ResponseEntity<Map<String, String>> rebuild() {
        indexService.rebuildIndex();
        return ResponseEntity.ok(Map.of("message", "Index rebuilt successfully"));
    }

    @PostMapping("/document/{id}")
    @Operation(summary = "Index single document by ID", description = "Extracts terms and updates the inverted index for a specific document")
    @ApiResponse(responseCode = "200", description = "Document indexed successfully")
    @ApiResponse(responseCode = "404", description = "Document not found")
    @ApiResponse(responseCode = "500", description = "Indexing failed")
    public ResponseEntity<Map<String, String>> indexDocument(@PathVariable UUID id) {
        indexService.indexDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document indexed successfully"));
    }

    @GetMapping("/status")
    @Operation(summary = "Get index status and statistics", description = "Exposes statistical counters from the inverted index without leaking raw structures")
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    public ResponseEntity<IndexStatusResponse> getStatus() {
        int docCount = invertedIndex.getDocumentCount();
        int vocabSize = invertedIndex.getVocabularySize();
        int uniqueTerms = vocabSize;
        long totalPostings = invertedIndex.getIndex().values().stream()
                .mapToLong(pList -> pList.getPostings().size())
                .sum();

        IndexStatusResponse status = new IndexStatusResponse(docCount, vocabSize, uniqueTerms, totalPostings);
        return ResponseEntity.ok(status);
    }
}
