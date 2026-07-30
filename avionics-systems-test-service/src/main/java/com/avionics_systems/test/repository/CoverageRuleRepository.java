package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.CoverageRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoverageRuleRepository extends JpaRepository<CoverageRule, UUID> {

    List<CoverageRule> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<CoverageRule> findByProjectIdAndEnabledTrue(UUID projectId);

    List<CoverageRule> findByProjectIdAndScope(UUID projectId, CoverageRule.Scope scope);

    Optional<CoverageRule> findByIdAndProjectId(UUID id, UUID projectId);

    List<CoverageRule> findByProjectIdAndRuleType(UUID projectId, CoverageRule.RuleType ruleType);

    void deleteByIdAndProjectId(UUID id, UUID projectId);
}