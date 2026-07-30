package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Project Component - Represents a component within a project
 * Matches Avionics Systems DC's COMPONENT table
 */
@Entity
@Table(name = "project_components", schema = "jira_issue")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "assignee_type", length = 20)
    @Builder.Default
    private String assigneeType = "PROJECT_LEAD";

    @Column(name = "default_assignee_id")
    private UUID defaultAssigneeId;

    @Column(name = "is_assignee_type_enabled")
    @Builder.Default
    private Boolean isAssigneeTypeEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Assignee types
    public static final String ASSIGNEE_TYPE_PROJECT_LEAD = "PROJECT_LEAD";
    public static final String ASSIGNEE_TYPE_COMPONENT_LEAD = "COMPONENT_LEAD";
    public static final String ASSIGNEE_TYPE_UNASSIGNED = "UNASSIGNED";
}
