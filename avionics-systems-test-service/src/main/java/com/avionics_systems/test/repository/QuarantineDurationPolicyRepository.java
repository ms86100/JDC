package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.QuarantineDurationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuarantineDurationPolicyRepository extends JpaRepository<QuarantineDurationPolicy, UUID> {

    List<QuarantineDurationPolicy> findByProjectId(UUID projectId);

    List<QuarantineDurationPolicy> findByProjectIdAndIsActiveTrue(UUID projectId);

    Optional<QuarantineDurationPolicy> findByProjectIdAndIsDefaultTrue(UUID projectId);

    List<QuarantineDurationPolicy> findByPolicyType(QuarantineDurationPolicy.PolicyType policyType);

    Optional<QuarantineDurationPolicy> findByProjectIdAndPolicyName(UUID projectId, String policyName);
}