package com.jira.test.repository;

import com.jira.test.entity.EnvironmentProvisioningRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnvironmentProvisioningRuleRepository extends JpaRepository<EnvironmentProvisioningRule, UUID> {

    List<EnvironmentProvisioningRule> findByProjectId(UUID projectId);

    List<EnvironmentProvisioningRule> findByProjectIdAndIsActiveTrue(UUID projectId);

    List<EnvironmentProvisioningRule> findByProviderType(String providerType);

    List<EnvironmentProvisioningRule> findByIsActiveTrueOrderByPriorityDesc();

    List<EnvironmentProvisioningRule> findByProjectIdAndProviderType(UUID projectId, String providerType);
}