package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.FlakyTestAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlakyTestAnalysisRepository extends JpaRepository<FlakyTestAnalysis, UUID> {

    Optional<FlakyTestAnalysis> findByTestId(UUID testId);

    List<FlakyTestAnalysis> findByCurrentStatus(String status);

    @Query("SELECT f FROM FlakyTestAnalysis f WHERE f.flakyScore >= :threshold ORDER BY f.flakyScore DESC")
    List<FlakyTestAnalysis> findByFlakyScoreGreaterThanEqual(@Param("threshold") BigDecimal threshold);

    @Query("SELECT f FROM FlakyTestAnalysis f WHERE f.currentStatus = 'quarantine_candidate' ORDER BY f.flakyScore DESC")
    List<FlakyTestAnalysis> findQuarantineCandidates();

    @Query("SELECT f FROM FlakyTestAnalysis f ORDER BY f.flakyScore DESC")
    List<FlakyTestAnalysis> findAllOrderByFlakyScoreDesc();

    @Query("SELECT f FROM FlakyTestAnalysis f WHERE f.passRateTrend = :trend ORDER BY f.flakyScore DESC")
    List<FlakyTestAnalysis> findByPassRateTrend(@Param("trend") String trend);
}