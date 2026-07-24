package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hlvvo_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HlvvoDefinition {

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

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "airbus_reference")
    private String airbusReference;

    @Column(name = "hlvvo_version")
    @Builder.Default
    private Integer hlvvoVersion = 1;

    @Column(name = "proofreading_data", columnDefinition = "JSONB")
    private String proofreadingData;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "specification_reference", columnDefinition = "TEXT")
    private String specificationReference;

    @Column(name = "component_ids", columnDefinition = "UUID[]")
    @Builder.Default
    private List<UUID> componentIds = List.of();

    @Column(name = "task_progress")
    @Builder.Default
    private Integer taskProgress = 0;

    @Column(name = "pts_link")
    private String ptsLink;

    @Column(name = "mfcl_link")
    private String mfclLink;

    @Column(name = "fix_version_id")
    private UUID fixVersionId;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> labels = List.of();

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
