package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "step_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(length = 20)
    @Builder.Default
    private String status = "NOT_RUN";

    @Column(name = "actual_result", columnDefinition = "TEXT")
    private String actualResult;

    @Column(name = "evidence_urls", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> evidenceUrls = List.of();

    @Column(name = "defect_key", length = 100)
    private String defectKey;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}