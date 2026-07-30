package com.avionics_systems.test.entity;

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

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "previous_coverage", precision = 5, scale = 2)
    private BigDecimal previousCoverage;

    @Column(name = "current_coverage", precision = 5, scale = 2)
    private BigDecimal currentCoverage;

    @Column(name = "drift", precision = 5, scale = 2)
    private BigDecimal drift;

    @Enumerated(EnumType.STRING)
    @Column(name = "drift_type", nullable = false, length = 20)
    @Builder.Default
    private DriftType driftType = DriftType.STABLE;

    @Column(name = "previous_test_count")
    private Integer previousTestCount;

    @Column(name = "current_test_count")
    private Integer currentTestCount;

    @Column(name = "affected_tests", columnDefinition = "JSONB")
    private String affectedTests; // JSON array of affected test objects

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "missing_coverage", columnDefinition = "JSONB")
    private String missingCoverage;

    @Column(name = "stale_coverage", columnDefinition = "JSONB")
    private String staleCoverage;

    @Column(name = "action_required")
    @Builder.Default
    private Boolean actionRequired = false;

    public enum DriftType {
        IMPROVED, DEGRADED, STABLE
    }
}