package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Project-level sprint permissions (mimics Jira GreenHopper).
 */
@Entity
@Table(name = "project_sprint_permissions", schema = "jira_plan", indexes = {
    @Index(name = "idx_project_sprint_permissions_project", columnList = "project_id"),
    @Index(name = "idx_project_sprint_permissions_key", columnList = "permission_key")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSprintPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;  // NULL for global permissions

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey;  // MANAGE_SPRINTS, START_STOP_SPRINTS, EDIT_SPRINT_NAME_AND_GOAL

    @Column(name = "principal_type", nullable = false, length = 20)
    private String principalType;  // USER, GROUP

    @Column(name = "principal_id", nullable = false)
    private UUID principalId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;
}