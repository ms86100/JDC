package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coverage_thresholds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "requirement_id")
    private UUID requirementId;

    @Column(name = "requirement_key")
    private String requirementKey;

    @Column(name = "minimum_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumCoverage;

    @Column(name = "warning_threshold", precision = 5, scale = 2)
    private BigDecimal warningThreshold;

    @Column(name = "current_coverage", precision = 5, scale = 2)
    private BigDecimal currentCoverage;

    @Column(name = "last_checked")
    @UpdateTimestamp
    private LocalDateTime lastChecked;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "alert_enabled")
    @Builder.Default
    private Boolean alertEnabled = true;

    @Column(name = "alert_sent")
    @Builder.Default
    private Boolean alertSent = false;

    public enum AlertLevel {
        OK,
        WARNING,
        CRITICAL
    }

    public AlertLevel getAlertLevel() {
        if (currentCoverage == null) return AlertLevel.OK;
        if (minimumCoverage != null && currentCoverage.compareTo(minimumCoverage) < 0) {
            return AlertLevel.CRITICAL;
        }
        if (warningThreshold != null && currentCoverage.compareTo(warningThreshold) < 0) {
            return AlertLevel.WARNING;
        }
        return AlertLevel.OK;
    }
}