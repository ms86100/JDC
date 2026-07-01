package com.jira.component.repository;

import com.jira.component.entity.ComponentAssignmentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentAssignmentRuleRepository extends JpaRepository<ComponentAssignmentRule, UUID> {

    List<ComponentAssignmentRule> findByComponentIdAndIsActiveTrue(UUID componentId);

    List<ComponentAssignmentRule> findByComponentId(UUID componentId);
}