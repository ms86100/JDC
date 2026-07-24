package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vvo_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VvoDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_key", length = 20)
    private String issueKey;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    @Builder.Default
    private String status = "NEW";

    @Column(name = "hlvvo_id")
    private UUID hlvvoId;

    @Column(name = "execution_responsible", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> executionResponsible = List.of();

    @Column(name = "execution_delegation", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> executionDelegation = List.of();

    @Column(name = "vvo_usage", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> vvoUsage = List.of();

    @Column(name = "vvo_scope", length = 30)
    private String vvoScope;

    @Column(name = "test_mean_type_requested", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> testMeanTypeRequested = List.of();

    @Column(name = "operational_conditions", columnDefinition = "TEXT")
    private String operationalConditions;

    @Column(name = "expected_results", columnDefinition = "TEXT")
    private String expectedResults;

    @Column(name = "real_system_needed", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> realSystemNeeded = List.of();

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> applicability = List.of();

    @Column(name = "supplier_applicability", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> supplierApplicability = List.of();

    @Column(name = "associated_requirements", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> associatedRequirements = List.of();

    @Column(name = "id_doors", length = 100)
    private String idDoors;

    @Column(name = "vvo_version")
    @Builder.Default
    private Integer vvoVersion = 1;

    @Column(name = "clone_source_id")
    private UUID cloneSourceId;

    @Column(name = "fix_version_id")
    private UUID fixVersionId;

    @Column(name = "milestone_target")
    private String milestoneTarget;

    @Column(name = "specification_reference", columnDefinition = "TEXT")
    private String specificationReference;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> labels = List.of();

    @Column(name = "component_ids", columnDefinition = "UUID[]")
    @Builder.Default
    private List<UUID> componentIds = List.of();

    @Builder.Default
    private Boolean archived = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
