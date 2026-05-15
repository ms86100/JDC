package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Workflow Transition - Enhanced with Jira DC features
 *
 * Represents a transition between workflow statuses with:
 * - Conditions (who can perform the transition)
 * - Validators (validation before transition completes)
 * - Post-functions (actions performed after transition)
 */
@Entity
@Table(name = "workflow_transitions", schema = "jira_workflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "from_status_id", nullable = false)
    private UUID fromStatusId;

    @Column(name = "to_status_id", nullable = false)
    private UUID toStatusId;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "icon", length = 100)
    private String icon;  // Icon key for UI

    // Validation rules
    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(name = "approval_group_id")
    private UUID approvalGroupId;  // Required approval group

    @Column(name = "allow_assignee_override", nullable = false)
    @Builder.Default
    private Boolean allowAssigneeOverride = true;

    @Column(name = "allow_unassign", nullable = false)
    @Builder.Default
    private Boolean allowUnassign = true;

    @Column(name = "fields_required", columnDefinition = "TEXT")
    private String fieldsRequired;  // JSON array of required fields

    @Column(name = "fields_updated", columnDefinition = "TEXT")
    private String fieldsUpdated;  // JSON array of fields to update automatically

    @Column(name = "fields_hidden", columnDefinition = "TEXT")
    private String fieldsHidden;  // JSON array of fields to hide

    @Column(name = "fields_auto_submit", nullable = false)
    @Builder.Default
    private Boolean fieldsAutoSubmit = false;

    // Security
    @Column(name = "permission_check", length = 50)
    private String permissionCheck;  // Permission required to perform transition

    @Column(name = "user_group_ids", columnDefinition = "TEXT")
    private String userGroupIds;  // JSON array of allowed group IDs

    // Linked Issue Transitions
    @Column(name = "remote_link_transition", nullable = false)
    @Builder.Default
    private Boolean remoteLinkTransition = false;  // Should linked issues transition too

    @Column(name = "remote_link_direction", length = 10)
    private String remoteLinkDirection;  // OUTWARD, INWARD, BOTH

    @Column(name = "remote_link_issue_link_type", length = 50)
    private String remoteLinkIssueLinkType;  // Blocks, relates to, etc.

    // Loop prevention
    @Column(name = "allow_loop", nullable = false)
    @Builder.Default
    private Boolean allowLoop = false;

    @Column(name = "max_loop_count")
    @Builder.Default
    private Integer maxLoopCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Transition operations
    public static final String OPERATION_AUTO = "AUTO";
    public static final String OPERATION_MANUAL = "MANUAL";
    public static final String OPERATION_SCRIPT = "SCRIPT";

    public boolean hasCondition() {
        return permissionCheck != null || (userGroupIds != null && !userGroupIds.isEmpty());
    }

    public boolean requiresFields() {
        return fieldsRequired != null && !fieldsRequired.isEmpty();
    }
}