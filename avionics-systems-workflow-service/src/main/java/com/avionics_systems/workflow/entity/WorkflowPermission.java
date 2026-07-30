package com.avionics_systems.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "workflow_permissions", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "permission_type", nullable = false, length = 50)
    private String permissionType;

    @Column(name = "permission_target_type", length = 20)
    private String permissionTargetType;

    @Column(name = "permission_target_id")
    private UUID permissionTargetId;

    @Column(name = "permission_value", length = 20)
    @Builder.Default
    private String permissionValue = "GRANT";

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;

    public static final String TYPE_VIEW = "VIEW";
    public static final String TYPE_EDIT = "EDIT";
    public static final String TYPE_ADMIN = "ADMIN";
    public static final String TYPE_PUBLISH = "PUBLISH";

    public static final String TARGET_USER = "USER";
    public static final String TARGET_GROUP = "GROUP";
    public static final String TARGET_PROJECT_ROLE = "PROJECT_ROLE";

    public static final String VALUE_GRANT = "GRANT";
    public static final String VALUE_DENY = "DENY";
}