package com.jira.notification.repository;

import com.jira.notification.entity.AutomationCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationConditionRepository extends JpaRepository<AutomationCondition, UUID> {

    List<AutomationCondition> findByRuleId(UUID ruleId);

    @Query("SELECT ac FROM AutomationCondition ac WHERE ac.ruleId = :ruleId AND ac.enabled = true ORDER BY ac.orderIndex")
    List<AutomationCondition> findEnabledByRuleId(@Param("ruleId") UUID ruleId);

    @Modifying
    @Query("DELETE FROM AutomationCondition ac WHERE ac.ruleId = :ruleId")
    void deleteAllByRuleId(@Param("ruleId") UUID ruleId);
}