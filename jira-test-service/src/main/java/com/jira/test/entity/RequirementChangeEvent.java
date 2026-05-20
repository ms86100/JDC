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

    @Column(name = "from_version", nullable = false)
    private Integer fromVersion;

    @Column(name = "to_version", nullable = false)
    private Integer toVersion;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;

    @Column(name = "field_changes", columnDefinition = "JSONB")
    private String fieldChanges; // [{field, oldValue, newValue}]

    @Column(name = "impact_assessment", columnDefinition = "JSONB")
    private String impactAssessment;

    @Column(name = "affected_tests", columnDefinition = "JSONB")
    private String affectedTests; // [{testId, impactLevel}]

    @Column(name = "notified_stakeholders", columnDefinition = "JSONB")
    private String notifiedStakeholders;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}