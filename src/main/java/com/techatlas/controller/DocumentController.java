package com.techatlas.controller;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document API", description = "Endpoints for managing search documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @Operation(summary = "Create a new document", description = "Creates a search document and calculates content hash")
    @ApiResponse(responseCode = "201", description = "Document created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    @ApiResponse(responseCode = "409", description = "Conflict: duplicate content hash")
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request) {
        DocumentResponse response = documentService.create(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all documents", description = "Retrieves all indexed documents")
    @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    public ResponseEntity<List<DocumentResponse>> listAll() {
        List<DocumentResponse> response = documentService.listAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a document by ID", description = "Retrieves details of a specific document")
    @ApiResponse(responseCode = "200", description = "Document found")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<DocumentResponse> retrieve(@PathVariable UUID id) {
        DocumentResponse response = documentService.retrieve(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing document", description = "Updates fields of an existing document")
    @ApiResponse(responseCode = "200", description = "Document updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    @ApiResponse(responseCode = "404", description = "Document not found")
    @ApiResponse(responseCode = "409", description = "Conflict: duplicate content hash")
    public ResponseEntity<DocumentResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateDocumentRequest request) {
        DocumentResponse response = documentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document by ID", description = "Deletes a specific document")
    @ApiResponse(responseCode = "204", description = "Document deleted successfully")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
