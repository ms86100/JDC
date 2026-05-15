package com.jira.issue.repository;

import com.jira.issue.entity.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResolutionRepository extends JpaRepository<Resolution, UUID> {
    Optional<Resolution> findByName(String name);
    Optional<Resolution> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}