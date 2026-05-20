package com.jira.test.repository;

import com.jira.test.entity.CoverageDriftRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoverageDriftRecordRepository extends JpaRepository<CoverageDriftRecord, UUID> {

    List<CoverageDriftRecord> findByRequirementIdOrderByAnalysisTimestampDesc(UUID requirementId);

    List<CoverageDriftRecord> findByActionRequiredTrue();
}