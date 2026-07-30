package com.avionics_systems.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "automation_rules", schema = "jira_notification")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "trigger_type", nullable = false, length = 100)
    private String triggerType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "is_system_rule")
    @Builder.Default
    private Boolean isSystemRule = false;

    @Column(name = "execution_count")
    @Builder.Default
    private Integer executionCount = 0;

    @Column(name = "last_executed_at")
    private OffsetDateTime lastExecutedAt;

    @Column(name = "last_status", length = 50)
    private String lastStatus;

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}