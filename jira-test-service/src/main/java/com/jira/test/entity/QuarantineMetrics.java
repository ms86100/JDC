package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quarantine_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quarantine_id", nullable = false)
    private UUID quarantineId;

    @Column(name = "metric_date", nullable = false)
    private LocalDateTime metricDate;

    @Column(name = "quarantine_age_days")
    private Integer quarantineAgeDays;

    @Column(name = "execution_count")
    @Builder.Default
    private Integer executionCount = 0;

    @Column(name = "pass_count")
    @Builder.Default
    private Integer passCount = 0;

    @Column(name = "fail_count")
    @Builder.Default
    private Integer failCount = 0;

    @Column(name = "flaky_score", precision = 5, scale = 2)
    private java.math.BigDecimal flakyScore;

    @Column(columnDefinition = "TEXT")
    private String notes;
}