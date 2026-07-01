package com.jira.project.repository;

import com.jira.project.entity.StatusDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusDefinitionRepository extends JpaRepository<StatusDefinition, UUID> {

    Optional<StatusDefinition> findByStatusKey(String statusKey);

    List<StatusDefinition> findByStatusCategoryOrderByStatusKeyAsc(String statusCategory);

    boolean existsByStatusKey(String statusKey);
}