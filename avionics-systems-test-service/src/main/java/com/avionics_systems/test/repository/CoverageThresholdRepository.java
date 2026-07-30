package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.CoverageThreshold;
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
public interface CoverageThresholdRepository extends JpaRepository<CoverageThreshold, UUID> {

    List<CoverageThreshold> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<CoverageThreshold> findByRequirementId(UUID requirementId);

    Optional<CoverageThreshold> findByProjectIdAndRequirementId(UUID projectId, UUID requirementId);

    List<CoverageThreshold> findByProjectIdAndAlertEnabledTrue(UUID projectId);

    @Query("SELECT ct FROM CoverageThreshold ct WHERE ct.projectId = :projectId " +
           "AND ct.alertEnabled = true AND ct.alertSent = false " +
           "AND ct.currentCoverage < ct.minimumCoverage")
    List<CoverageThreshold> findUnalertedBreaches(@Param("projectId") UUID projectId);

    @Query("SELECT ct FROM CoverageThreshold ct WHERE ct.projectId = :projectId " +
           "AND ct.alertEnabled = true AND ct.lastChecked < :since")
    List<CoverageThreshold> findStaleThresholds(
            @Param("projectId") UUID projectId,
            @Param("since") LocalDateTime since);

    List<CoverageThreshold> findByProjectIdAndCurrentCoverageLessThan(
            UUID projectId, BigDecimal threshold);
}