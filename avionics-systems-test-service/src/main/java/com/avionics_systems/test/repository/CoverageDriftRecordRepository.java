package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.CoverageDriftRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoverageDriftRecordRepository extends JpaRepository<CoverageDriftRecord, UUID> {

    List<CoverageDriftRecord> findByRequirementIdOrderByDetectedAtDesc(UUID requirementId);

    Optional<CoverageDriftRecord> findFirstByRequirementIdOrderByDetectedAtDesc(UUID requirementId);

    List<CoverageDriftRecord> findByActionRequiredTrue();

    List<CoverageDriftRecord> findByProjectIdAndActionRequiredTrue(UUID projectId);

    List<CoverageDriftRecord> findByProjectIdAndDriftType(UUID projectId, CoverageDriftRecord.DriftType driftType);

    List<CoverageDriftRecord> findByProjectIdAndDetectedAtAfterOrderByDetectedAtDesc(UUID projectId, LocalDateTime since);

    List<CoverageDriftRecord> findByRequirementIdAndDetectedAtAfterOrderByDetectedAtAsc(UUID requirementId, LocalDateTime since);

    @Query("SELECT cdr FROM CoverageDriftRecord cdr WHERE cdr.projectId = :projectId ORDER BY cdr.detectedAt DESC")
    List<CoverageDriftRecord> findAllByProjectIdOrderByDetectedAtDesc(@Param("projectId") UUID projectId, Pageable pageable);

    @Query("SELECT AVG(cdr.drift) FROM CoverageDriftRecord cdr WHERE cdr.requirementId = :requirementId AND cdr.detectedAt > :since")
    BigDecimal findAverageDriftByRequirementId(@Param("requirementId") UUID requirementId, @Param("since") LocalDateTime since);
}