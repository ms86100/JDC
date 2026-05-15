package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "workflow_content", columnDefinition = "TEXT")
    private String workflowContent;

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "is_draft")
    private Boolean isDraft = false;

    private Integer version;

    @Column(name = "status_categories", columnDefinition = "TEXT")
    private String statusCategories;
}