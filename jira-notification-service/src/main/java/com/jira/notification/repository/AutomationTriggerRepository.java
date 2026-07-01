package com.jira.notification.repository;

import com.jira.notification.entity.AutomationTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationTriggerRepository extends JpaRepository<AutomationTrigger, UUID> {

    List<AutomationTrigger> findByRuleId(UUID ruleId);

    @Query("SELECT at FROM AutomationTrigger at WHERE at.ruleId = :ruleId AND at.enabled = true ORDER BY at.orderIndex")
    List<AutomationTrigger> findEnabledByRuleId(@Param("ruleId") UUID ruleId);

    @Modifying
    @Query("DELETE FROM AutomationTrigger at WHERE at.ruleId = :ruleId")
    void deleteAllByRuleId(@Param("ruleId") UUID ruleId);

    @Query("SELECT at FROM AutomationTrigger at WHERE at.triggerType = :triggerType AND at.enabled = true")
    List<AutomationTrigger> findByTriggerTypeAndEnabled(@Param("triggerType") String triggerType);
}