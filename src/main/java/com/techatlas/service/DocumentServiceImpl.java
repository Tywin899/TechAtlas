package com.techatlas.service;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.Document;
import com.techatlas.exception.DocumentNotFoundException;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.mapper.DocumentMapper;
import com.techatlas.model.InvertedIndex;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.repository.DocumentRepository;
import com.techatlas.util.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.techatlas.cache.CacheService;
import com.techatlas.config.RedisCacheProperties;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final InvertedIndex invertedIndex;
    private final CacheService cacheService;
    private final RedisCacheProperties redisCacheProperties;

    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            DocumentMapper documentMapper,
            InvertedIndex invertedIndex,
            CacheService cacheService,
            RedisCacheProperties redisCacheProperties) {
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
        this.invertedIndex = invertedIndex;
        this.cacheService = cacheService;
        this.redisCacheProperties = redisCacheProperties;
    }

    @Override
    @Transactional
    public DocumentResponse create(CreateDocumentRequest request) {
        String contentHash = HashUtil.calculateSha256(request.content());
        if (documentRepository.existsByContentHash(contentHash)) {
            throw new DuplicateDocumentException("Document with duplicate content hash already exists: " + contentHash);
        }

        Document document = documentMapper.toEntity(request);
        Document saved = documentRepository.saveAndFlush(document);
        cacheService.clearAllSearchCaches();
        return documentMapper.toResponse(saved);
    }

    @Override
    public DocumentResponse retrieve(UUID id) {
        String key = "document:" + id;
        Optional<DocumentResponse> cached = cacheService.get(key, DocumentResponse.class);
        if (cached.isPresent()) {
            cacheService.incrementDocumentHits();
            return cached.get();
        }
        cacheService.incrementDocumentMisses();
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + id));
        DocumentResponse response = documentMapper.toResponse(document);
        cacheService.put(key, response, redisCacheProperties.getDocument().getTtlSeconds(), TimeUnit.SECONDS);
        return response;
    }

    @Override
    @Transactional
    public DocumentResponse update(UUID id, UpdateDocumentRequest request) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + id));

        String newHash = HashUtil.calculateSha256(request.content());
        if (!document.getContentHash().equals(newHash) && documentRepository.existsByContentHash(newHash)) {
            throw new DuplicateDocumentException("Another document with duplicate content hash already exists: " + newHash);
        }

        documentMapper.updateEntityFromDto(request, document);
        
        // Reset status to PENDING_INDEX and remove from index to avoid stale data
        document.setStatus(DocumentStatus.PENDING_INDEX);
        document.setIndexedAt(null);
        invertedIndex.removeDocument(id);

        Document saved = documentRepository.saveAndFlush(document);
        cacheService.evict("document:" + id);
        cacheService.clearAllSearchCaches();
        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!documentRepository.existsById(id)) {
            throw new DocumentNotFoundException("Document not found with ID: " + id);
        }
        invertedIndex.removeDocument(id);
        documentRepository.deleteById(id);
        cacheService.evict("document:" + id);
        cacheService.clearAllSearchCaches();
    }

    @Override
    public List<DocumentResponse> listAll() {
        return documentRepository.findAll().stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByContentHash(String contentHash) {
        return documentRepository.existsByContentHash(contentHash);
    }

    @Override
    public Optional<DocumentResponse> findByContentHash(String contentHash) {
        return documentRepository.findByContentHash(contentHash)
                .map(documentMapper::toResponse);
    }

    @Override
    @Transactional
    public DocumentResponse updateStatus(UUID id, com.techatlas.entity.DocumentStatus status, java.time.LocalDateTime indexedAt) {
        com.techatlas.entity.Document document = documentRepository.findById(id)
                .orElseThrow(() -> new com.techatlas.exception.DocumentNotFoundException("Document not found with ID: " + id));
        document.setStatus(status);
        document.setIndexedAt(indexedAt);
        com.techatlas.entity.Document saved = documentRepository.saveAndFlush(document);
        return documentMapper.toResponse(saved);
    }
}
