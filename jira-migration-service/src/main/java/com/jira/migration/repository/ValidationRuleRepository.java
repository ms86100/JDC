package com.jira.migration.repository;

import com.jira.migration.entity.ValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ValidationRuleRepository extends JpaRepository<ValidationRule, UUID> {

    List<ValidationRule> findByEntityTypeAndIsActiveTrueOrderByDisplayOrderAsc(String entityType);
}
