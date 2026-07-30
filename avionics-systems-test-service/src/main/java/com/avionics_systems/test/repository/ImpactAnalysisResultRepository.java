package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ImpactAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImpactAnalysisResultRepository extends JpaRepository<ImpactAnalysisResult, UUID> {

    List<ImpactAnalysisResult> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<ImpactAnalysisResult> findTop10ByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ImpactAnalysisResult> findByTriggerId(UUID triggerId);

    List<ImpactAnalysisResult> findByTriggerType(String triggerType);
}