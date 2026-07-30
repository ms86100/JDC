package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.ValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ValidationRuleRepository extends JpaRepository<ValidationRule, UUID> {

    List<ValidationRule> findByEntityTypeAndIsActiveTrueOrderByDisplayOrderAsc(String entityType);
}
