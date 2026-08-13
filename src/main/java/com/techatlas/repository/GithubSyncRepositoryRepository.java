package com.techatlas.repository;

import com.techatlas.entity.GithubSyncRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubSyncRepositoryRepository extends JpaRepository<GithubSyncRepository, UUID> {
    Optional<GithubSyncRepository> findByGithubRepoId(Long githubRepoId);
    boolean existsByGithubRepoId(Long githubRepoId);
}
