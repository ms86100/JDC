package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_plan_id")
    private UUID testPlanId;

    @Column(name = "test_set_id")
    private UUID testSetId;

    @Column(name = "test_id")
    private UUID testId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    @Builder.Default
    private String status = "RUNNING";

    @Column(name = "test_env", length = 50)
    private String testEnv;

    @Column(name = "tester_id")
    private UUID testerId;

    @Column(name = "test_cycle", length = 100)
    private String testCycle;

    @Column(name = "ci_build_url")
    private String ciBuildUrl;

    @Column(name = "ci_job_id")
    private String ciJobId;

    @Column(name = "total_tests")
    @Builder.Default
    private Integer totalTests = 0;

    @Column(name = "passed_tests")
    @Builder.Default
    private Integer passedTests = 0;

    @Column(name = "failed_tests")
    @Builder.Default
    private Integer failedTests = 0;

    @Column(name = "blocked_tests")
    @Builder.Default
    private Integer blockedTests = 0;

    @Column(name = "not_run_tests")
    @Builder.Default
    private Integer notRunTests = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}