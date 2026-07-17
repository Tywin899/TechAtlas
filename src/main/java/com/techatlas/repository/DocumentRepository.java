package com.techatlas.repository;

import com.techatlas.entity.Document;
import com.techatlas.entity.DocumentStatus;
import com.techatlas.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByStatus(DocumentStatus status);
    List<Document> findBySource(SourceType source);
    Optional<Document> findByContentHash(String contentHash);
    boolean existsByContentHash(String contentHash);
    List<Document> findByCategory(String category);
    List<Document> findByLanguage(String language);
}
