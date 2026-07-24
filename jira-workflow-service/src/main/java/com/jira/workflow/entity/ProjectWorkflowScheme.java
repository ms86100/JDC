package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_workflow_schemes", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectWorkflowScheme {

    @Id
    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "scheme_id", nullable = false)
    private UUID schemeId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
