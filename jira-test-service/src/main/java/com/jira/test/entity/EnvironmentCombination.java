package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "environment_combinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentCombination {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "matrix_id", nullable = false)
    private UUID matrixId;

    @Column(name = "combination_index")
    @Builder.Default
    private Integer combinationIndex = 0;

    @Column(name = "combination_data", columnDefinition = "JSONB", nullable = false)
    private String combinationData; // {browser: "Chrome", os: "Windows", region: "US"}

    @Column(name = "is_valid")
    @Builder.Default
    private Boolean isValid = true;

    @Column(name = "validation_errors", columnDefinition = "JSONB")
    private String validationErrors; // [{rule: "incompatible", details: "..."}]

    @Column(name = "provisioned_config", columnDefinition = "JSONB")
    private String provisionedConfig; // Actual provisioned configuration

    @Column(name = "provisioning_status", length = 50)
    @Builder.Default
    private String provisioningStatus = "PENDING"; // PENDING, PROVISIONING, PROVISIONED, FAILED

    @Column(name = "provisioned_at")
    private LocalDateTime provisionedAt;

    @Column(name = "provisioning_error", columnDefinition = "TEXT")
    private String provisioningError;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}