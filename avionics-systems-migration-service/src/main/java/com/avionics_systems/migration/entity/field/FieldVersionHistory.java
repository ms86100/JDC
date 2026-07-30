package com.avionics_systems.migration.entity.field;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Field version history entity for tracking field definition changes.
 * Provides audit trail and rollback capability for field modifications.
 */
@Entity
@Table(name = "field_version_history", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldVersionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "field_definition_id", nullable = false)
    private UUID fieldDefinitionId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;

    @Column(name = "field_key", length = 255)
    private String fieldKey;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", length = 50)
    private FieldDefinition.FieldType fieldType;

    @Enumerated(EnumType.STRING)
    @Column(name = "renderer", length = 100)
    private FieldDefinition.FieldRenderer renderer;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_region", length = 50)
    private FieldDefinition.ScreenRegion screenRegion;

    @Column(name = "schema_definition", columnDefinition = "jsonb")
    private String schemaDefinition;

    @Column(name = "renderer_config", columnDefinition = "jsonb")
    private String rendererConfig;

    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private String validationRules;

    @Column(name = "options", columnDefinition = "jsonb")
    private String options;

    @Column(name = "searchable")
    private Boolean searchable;

    @Column(name = "sortable")
    private Boolean sortable;

    @Column(name = "filterable")
    private Boolean filterable;

    @Column(name = "required")
    private Boolean required;

    @Column(name = "read_only")
    private Boolean readOnly;

    @Column(name = "hidden")
    private Boolean hidden;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}