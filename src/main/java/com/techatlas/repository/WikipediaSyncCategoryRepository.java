package com.techatlas.repository;

import com.techatlas.entity.WikipediaSyncCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WikipediaSyncCategoryRepository extends JpaRepository<WikipediaSyncCategory, UUID> {
    Optional<WikipediaSyncCategory> findByCategoryName(String categoryName);
    boolean existsByCategoryName(String categoryName);
}
