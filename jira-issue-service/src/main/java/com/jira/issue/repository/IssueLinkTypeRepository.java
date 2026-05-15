package com.jira.issue.repository;

import com.jira.issue.entity.IssueLinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueLinkTypeRepository extends JpaRepository<IssueLinkType, UUID> {
    Optional<IssueLinkType> findByName(String name);
    Optional<IssueLinkType> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}