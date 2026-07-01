package com.jira.migration.entity.field;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Context for custom field - defines which projects/types a custom field applies to.
 */
@Entity
@Table(name = "custom_field_contexts", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldContext {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "custom_field_id", nullable = false)
    private UUID customFieldId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "all_projects")
    @Builder.Default
    private Boolean allProjects = true;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "project_ids", columnDefinition = "uuid[]")
    private UUID[] projectIds;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "issue_type_ids", columnDefinition = "uuid[]")
    private UUID[] issueTypeIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_values", columnDefinition = "jsonb")
    private Map<String, Object> defaultValues;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}