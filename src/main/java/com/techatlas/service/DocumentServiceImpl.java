package com.techatlas.service;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.Document;
import com.techatlas.exception.DocumentNotFoundException;
import com.techatlas.exception.DuplicateDocumentException;
import com.techatlas.mapper.DocumentMapper;
import com.techatlas.repository.DocumentRepository;
import com.techatlas.util.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;

    public DocumentServiceImpl(DocumentRepository documentRepository, DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
    }

    @Override
    @Transactional
    public DocumentResponse create(CreateDocumentRequest request) {
        String contentHash = HashUtil.calculateSha256(request.content());
        if (documentRepository.existsByContentHash(contentHash)) {
            throw new DuplicateDocumentException("Document with duplicate content hash already exists: " + contentHash);
        }

        Document document = documentMapper.toEntity(request);
        Document saved = documentRepository.save(document);
        return documentMapper.toResponse(saved);
    }

    @Override
    public DocumentResponse retrieve(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + id));
        return documentMapper.toResponse(document);
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
        Document saved = documentRepository.save(document);
        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!documentRepository.existsById(id)) {
            throw new DocumentNotFoundException("Document not found with ID: " + id);
        }
        documentRepository.deleteById(id);
    }

    @Override
    public List<DocumentResponse> listAll() {
        return documentRepository.findAll().stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }
}
