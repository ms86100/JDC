package com.avionics_systems.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "workflow_sharing", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSharing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "scheme_id")
    private UUID schemeId;

    @Column(name = "shared_by")
    private UUID sharedBy;

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;
public UUID getSchemeId() {
        return schemeId;
    }
}