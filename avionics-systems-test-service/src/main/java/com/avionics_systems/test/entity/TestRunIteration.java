package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_run_iteration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRunIteration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_run_id", nullable = false)
    private UUID testRunId;

    @Column(name = "iteration_index", nullable = false)
    @Builder.Default
    private Integer iterationIndex = 0;

    @Column(name = "data_row", columnDefinition = "JSONB")
    private String dataRow;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "step_statuses", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> stepStatuses = List.of();

    @Column(name = "passed_steps")
    @Builder.Default
    private Integer passedSteps = 0;

    @Column(name = "failed_steps")
    @Builder.Default
    private Integer failedSteps = 0;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column
    private Integer duration;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
