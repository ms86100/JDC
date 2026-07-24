package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_jobs")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobEntity {

    public enum JobStatus {
        SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String jobId;

    @Column(nullable = false)
    private String jobName;

    private String description;

    @Column(nullable = false)
    private String jobType;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.SCHEDULED;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "next_run_time")
    private LocalDateTime nextRunTime;

    @Column(name = "last_run_time")
    private LocalDateTime lastRunAt;

    @Column(name = "last_duration_ms")
    private Long lastDurationMs;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "is_running")
    private Boolean isRunning = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}