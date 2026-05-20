package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_shared_step_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSharedStepMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "test_step_index", nullable = false)
    private Integer testStepIndex;

    @Column(name = "shared_step_id", nullable = false)
    private UUID sharedStepId;

    @Column(name = "shared_step_version_id")
    private UUID sharedStepVersionId;

    @Column(name = "embedded_snapshot", columnDefinition = "JSONB")
    private String embeddedSnapshot; // Frozen copy at time of linking

    @Column(name = "parameters", columnDefinition = "JSONB")
    private String parameters; // JSON object for step parameters

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}