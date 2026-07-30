package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quarantine_duration_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineDurationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "policy_name", nullable = false, length = 255)
    private String policyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "policy_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PolicyType policyType;

    @Column(name = "duration_rule", columnDefinition = "JSONB")
    private String durationRule; // {type: "TEMPORARY", durationDays: 30}

    @Column(name = "auto_restore_config", columnDefinition = "JSONB")
    private String autoRestoreConfig; // {enabled: true, minPassCount: 5, minDaysElapsed: 7}

    @Column(name = "review_config", columnDefinition = "JSONB")
    private String reviewConfig; // {required: true, reviewAfterDays: 14}

    @Column(name = "escalation_config", columnDefinition = "JSONB")
    private String escalationConfig; // {enabled: true, escalateAfterDays: 30}

    @Column(name = "conditions", columnDefinition = "JSONB")
    private String conditions; // Additional conditions for when to apply

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "current_usage_count")
    @Builder.Default
    private Integer currentUsageCount = 0;

    @Column(name = "historical_usage_count")
    @Builder.Default
    private Integer historicalUsageCount = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PolicyType {
        FLAKY_TEST,
        ENVIRONMENTAL,
        DATA_DEPENDENCY,
        INFRASTRUCTURE,
        THIRD_PARTY,
        MANUAL_OVERRIDE,
        CUSTOM
    }
}