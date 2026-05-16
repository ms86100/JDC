package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Template Workflow Transition - Defines transitions between statuses in a template workflow
 */
@Entity
@Table(name = "template_workflow_transitions", schema = "jira_project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateWorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "from_status_key", nullable = false, length = 20)
    private String fromStatusKey;

    @Column(name = "to_status_key", nullable = false, length = 20)
    private String toStatusKey;

    @Column(name = "transition_name", length = 100)
    private String transitionName;

    @Column(name = "transition_icon", length = 50)
    private String transitionIcon;

    @Column(name = "allow_backward")
    @Builder.Default
    private Boolean allowBackward = false;

    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}