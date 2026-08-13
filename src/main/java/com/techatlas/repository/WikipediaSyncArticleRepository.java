package com.techatlas.repository;

import com.techatlas.entity.WikipediaSyncArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WikipediaSyncArticleRepository extends JpaRepository<WikipediaSyncArticle, UUID> {
    Optional<WikipediaSyncArticle> findByArticleTitle(String articleTitle);
    boolean existsByArticleTitle(String articleTitle);
}
