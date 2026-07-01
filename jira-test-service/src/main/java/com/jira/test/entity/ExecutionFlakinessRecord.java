package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.UUID;

@Entity
@Table(name = "execution_flakiness_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionFlakinessRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "is_flaky_execution")
    private Boolean isFlakyExecution;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "environment_id")
    private UUID environmentId;

    @Column(name = "execution_duration_ms")
    private Integer executionDurationMs;

    @Column(name = "retry_attempt")
    @Builder.Default
    private Integer retryAttempt = 0;

    @CreationTimestamp
    @Column(name = "analyzed_at")
    private java.time.LocalDateTime analyzedAt;
}