package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SharedStep - Reusable test steps library
 */
@Entity
@Table(name = "shared_steps", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_ss_project", columnList = "project_id"),
        @Index(name = "idx_ss_name", columnList = "name")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "step_type", nullable = false, length = 20)
    private String stepType; // GIVEN, WHEN, THEN, AND, BUT

    @Column(name = "description_template", nullable = false, columnDefinition = "TEXT")
    private String descriptionTemplate; // Template with placeholders

    @Column(name = "test_data_template", columnDefinition = "TEXT")
    private String testDataTemplate;

    @Column(name = "expected_result_template", columnDefinition = "TEXT")
    private String expectedResultTemplate;

    // Parameter definitions
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, String>> parameters;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}