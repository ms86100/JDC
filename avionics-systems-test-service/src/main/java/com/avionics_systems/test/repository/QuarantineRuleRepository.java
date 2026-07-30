package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.QuarantineRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuarantineRuleRepository extends JpaRepository<QuarantineRule, UUID> {

    List<QuarantineRule> findByProjectId(UUID projectId);

    List<QuarantineRule> findByProjectIdAndIsActiveTrue(UUID projectId);

    List<QuarantineRule> findByIsActiveTrue();
}