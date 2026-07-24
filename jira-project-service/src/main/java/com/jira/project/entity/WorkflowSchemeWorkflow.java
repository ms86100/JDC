package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "workflow_scheme_workflows", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(WorkflowSchemeWorkflow.WorkflowSchemeWorkflowId.class)
public class WorkflowSchemeWorkflow {

    @Id
    @Column(name = "scheme_id")
    private UUID schemeId;

    @Id
    @Column(name = "workflow_name")
    private String workflowName;

    @Column(name = "issue_type_name")
    private String issueTypeName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowSchemeWorkflowId implements Serializable {
        private UUID schemeId;
        private String workflowName;
    }
}