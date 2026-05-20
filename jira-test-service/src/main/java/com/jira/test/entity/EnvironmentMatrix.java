package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "environment_matrix")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "dimension_configs", columnDefinition = "JSONB", nullable = false)
    private String dimensionConfigs; // [{name: "Browser", values: ["Chrome", "Firefox"]}, ...]

    @Column(name = "filter_rules", columnDefinition = "JSONB")
    private String filterRules; // Rules to filter combinations

    @Column(name = "conflict_rules", columnDefinition = "JSONB")
    private String conflictRules; // Rules to detect conflicts

    @Column(name = "total_combinations")
    @Builder.Default
    private Integer totalCombinations = 0;

    @Column(name = "valid_combinations")
    @Builder.Default
    private Integer validCombinations = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}