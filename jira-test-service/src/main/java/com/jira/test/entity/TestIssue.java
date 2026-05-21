package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "test_type", length = 50)
    @Builder.Default
    private String testType = "MANUAL";

    @Column(length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> labels = List.of();

    @Column(length = 20)
    private String priority;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "requirement_keys", columnDefinition = "TEXT[]")
    private List<String> requirementKeys;

    @Column(name = "gherkin_feature_key")
    private String gherkinFeatureKey;

    @Column(name = "gherkin_scenario_id")
    private String gherkinScenarioId;

    @Column(name = "test_set_id")
    private UUID testSetId;

    @Column
    @Builder.Default
    private Boolean archived = false;

    @Column(name = "folder_id")
    private UUID folderId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}