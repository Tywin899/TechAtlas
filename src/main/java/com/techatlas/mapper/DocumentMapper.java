package com.techatlas.mapper;

import com.techatlas.dto.CreateDocumentRequest;
import com.techatlas.dto.DocumentResponse;
import com.techatlas.dto.UpdateDocumentRequest;
import com.techatlas.entity.Document;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.util.HashUtil;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public Document toEntity(CreateDocumentRequest request) {
        if (request == null) {
            return null;
        }
        Document document = new Document();
        document.setTitle(request.title());
        document.setContent(request.content());
        document.setUrl(request.url());
        document.setSource(request.source());
        document.setCategory(request.category());
        document.setAuthor(request.author());
        document.setLanguage(request.language());
        document.setMetadata(request.metadata());
        
        document.setContentHash(HashUtil.calculateSha256(request.content()));
        document.setStatus(DocumentStatus.PENDING_INDEX);
        
        return document;
    }

    public void updateEntityFromDto(UpdateDocumentRequest request, Document document) {
        if (request == null || document == null) {
            return;
        }
        document.setTitle(request.title());
        document.setContent(request.content());
        document.setUrl(request.url());
        document.setSource(request.source());
        document.setCategory(request.category());
        document.setAuthor(request.author());
        document.setLanguage(request.language());
        document.setMetadata(request.metadata());
        document.setStatus(request.status());
        
        document.setContentHash(HashUtil.calculateSha256(request.content()));
    }

    public DocumentResponse toResponse(Document document) {
        if (document == null) {
            return null;
        }
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getUrl(),
                document.getSource(),
                document.getCategory(),
                document.getAuthor(),
                document.getLanguage(),
                document.getContentHash(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getIndexedAt(),
                document.getMetadata()
        );
    }
}
