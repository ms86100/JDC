package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "requirement_change_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementChangeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @Column(name = "version_id")
    private UUID versionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;

    @Column(name = "affected_fields", columnDefinition = "JSONB")
    private String affectedFields; // JSON array of affected field objects

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_level", length = 20)
    @Builder.Default
    private ImpactLevel impactLevel = ImpactLevel.LOW;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "from_version")
    private Integer fromVersion;

    @Column(name = "to_version")
    private Integer toVersion;

    @Column(name = "field_changes", columnDefinition = "JSONB")
    private String fieldChanges;

    @Column(name = "impact_assessment", columnDefinition = "JSONB")
    private String impactAssessment;

    @Column(name = "affected_tests", columnDefinition = "JSONB")
    private String affectedTests;

    @Column(name = "notified_stakeholders", columnDefinition = "JSONB")
    private String notifiedStakeholders;

    public enum ChangeType {
        ADDED, MODIFIED, DELETED, DEPRECATED
    }

    public enum ImpactLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}