package com.techatlas.repository;

import com.techatlas.entity.StackOverflowSyncQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StackOverflowSyncQuestionRepository extends JpaRepository<StackOverflowSyncQuestion, UUID> {
    Optional<StackOverflowSyncQuestion> findByQuestionId(Long questionId);
    boolean existsByQuestionId(Long questionId);
}
