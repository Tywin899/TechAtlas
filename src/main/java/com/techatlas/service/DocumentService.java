package com.techatlas.service;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.UpdateDocumentRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentService {
    DocumentResponse create(CreateDocumentRequest request);
    DocumentResponse retrieve(UUID id);
    DocumentResponse update(UUID id, UpdateDocumentRequest request);
    void delete(UUID id);
    List<DocumentResponse> listAll();
    boolean existsByContentHash(String contentHash);
    Optional<DocumentResponse> findByContentHash(String contentHash);
    DocumentResponse updateStatus(UUID id, com.techatlas.entity.DocumentStatus status, java.time.LocalDateTime indexedAt);
}
