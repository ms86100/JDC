package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Legacy admin view of workflow schemes. Canonical data lives in jira-workflow-service.
 */
@Entity
@Table(name = "workflow_schemes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSchemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_workflow_id")
    private String defaultWorkflowId;

    @Column(name = "issue_type_mappings", columnDefinition = "TEXT")
    private String issueTypeMappings;

    private Integer projectCount;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}