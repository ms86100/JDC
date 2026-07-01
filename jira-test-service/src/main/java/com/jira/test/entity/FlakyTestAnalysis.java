package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flaky_test_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlakyTestAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false, unique = true)
    private UUID testId;

    @Column(name = "total_executions")
    @Builder.Default
    private Integer totalExecutions = 0;

    @Column(name = "total_failures")
    @Builder.Default
    private Integer totalFailures = 0;

    @Column(name = "total_passes")
    @Builder.Default
    private Integer totalPasses = 0;

    @Column(name = "flaky_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal flakyScore = BigDecimal.ZERO;

    @Column(name = "pass_rate_trend", length = 20)
    @Builder.Default
    private String passRateTrend = "stable"; // improving, stable, degrading

    @Column(name = "first_flaky_occurrence")
    private LocalDateTime firstFlakyOccurrence;

    @Column(name = "last_flaky_occurrence")
    private LocalDateTime lastFlakyOccurrence;

    @Column(name = "current_status", length = 50)
    @Builder.Default
    private String currentStatus = "stable"; // stable, flaky, quarantine_candidate

    @Column(name = "confidence_level", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal confidenceLevel = BigDecimal.ZERO;

    @Column(name = "analysis_window_days")
    @Builder.Default
    private Integer analysisWindowDays = 30;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}