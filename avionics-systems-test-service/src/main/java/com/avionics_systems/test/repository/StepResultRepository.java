package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.StepResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StepResultRepository extends JpaRepository<StepResult, UUID> {

    List<StepResult> findByExecutionIdOrderByStepId(UUID executionId);

    List<StepResult> findByDefectKeyIsNotNull();

    List<StepResult> findByExecutionId(UUID executionId);
}