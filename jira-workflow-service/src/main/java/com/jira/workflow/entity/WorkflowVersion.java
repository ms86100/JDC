package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_versions", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "workflow_snapshot", columnDefinition = "TEXT", nullable = false)
    private String workflowSnapshot;

    @Column(name = "statuses_snapshot", columnDefinition = "TEXT")
    private String statusesSnapshot;

    @Column(name = "transitions_snapshot", columnDefinition = "TEXT")
    private String transitionsSnapshot;

    @Column(name = "conditions_snapshot", columnDefinition = "TEXT")
    private String conditionsSnapshot;

    @Column(name = "validators_snapshot", columnDefinition = "TEXT")
    private String validatorsSnapshot;

    @Column(name = "post_functions_snapshot", columnDefinition = "TEXT")
    private String postFunctionsSnapshot;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "change_description")
    private String changeDescription;

    @Column(name = "change_type")
    private String changeType;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}