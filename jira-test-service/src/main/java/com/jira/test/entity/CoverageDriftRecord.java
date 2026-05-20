package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coverage_drift_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageDriftRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @CreationTimestamp
    @Column(name = "analysis_timestamp")
    private LocalDateTime analysisTimestamp;

    @Column(name = "previous_coverage_score", precision = 5, scale = 2)
    private BigDecimal previousCoverageScore;

    @Column(name = "current_coverage_score", precision = 5, scale = 2)
    private BigDecimal currentCoverageScore;

    @Column(name = "drift_type", length = 50)
    private String driftType; // improved, degraded, stable

    @Column(name = "missing_coverage", columnDefinition = "JSONB")
    private String missingCoverage; // Tests that should exist

    @Column(name = "stale_coverage", columnDefinition = "JSONB")
    private String staleCoverage; // Tests no longer relevant

    @Column(name = "action_required")
    @Builder.Default
    private Boolean actionRequired = false;
}