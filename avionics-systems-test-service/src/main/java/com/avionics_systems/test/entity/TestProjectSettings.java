package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_project_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestProjectSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false, unique = true)
    private UUID projectId;

    @Column(columnDefinition = "JSONB")
    @Builder.Default
    private String settings = "{}";

    @Column(name = "default_test_type", length = 50)
    @Builder.Default
    private String defaultTestType = "MANUAL";

    @Column(name = "default_priority", length = 50)
    @Builder.Default
    private String defaultPriority = "MEDIUM";

    @Column(name = "default_test_status", length = 50)
    @Builder.Default
    private String defaultTestStatus = "DRAFT";

    @Column(name = "auto_create_execution")
    @Builder.Default
    private Boolean autoCreateExecution = false;

    @Column(name = "require_approval")
    @Builder.Default
    private Boolean requireApproval = false;

    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 365;

    @Column(name = "max_steps_per_test")
    @Builder.Default
    private Integer maxStepsPerTest = 100;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
