package com.avionics_systems.migration.entity.field;

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
import java.util.*;

/**
 * Registry for plugin-specific field definitions.
 * Enables dynamic plugin field handling and future extensibility.
 */
@Entity
@Table(name = "plugin_field_registry", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginFieldRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plugin_key", nullable = false, length = 255)
    private String pluginKey;

    @Column(name = "plugin_name", nullable = false, length = 255)
    private String pluginName;

    @Column(name = "plugin_version", length = 50)
    private String pluginVersion;

    @Column(name = "field_key", nullable = false, length = 255)
    private String fieldKey;

    @Column(name = "field_type", nullable = false, length = 100)
    private String fieldType;

    @Column(name = "jira_field_key", length = 255)
    private String legacyFieldKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_mapping", columnDefinition = "jsonb")
    private Map<String, Object> schemaMapping;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "import_mapping", columnDefinition = "jsonb")
    private Map<String, Object> importMapping;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "export_mapping", columnDefinition = "jsonb")
    private Map<String, Object> exportMapping;

    @Column(name = "searchable")
    @Builder.Default
    private Boolean searchable = true;

    @Column(name = "navigable")
    @Builder.Default
    private Boolean navigable = true;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "clauses", columnDefinition = "varchar[]")
    private String[] clauses;

    @Column(name = "field_definition_id")
    private UUID fieldDefinitionId;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "deployed")
    @Builder.Default
    private Boolean deployed = true;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    public enum FieldType {
        TEXT,
        NUMBER,
        FLOAT,
        DATE,
        DATETIME,
        SELECT,
        MULTI_SELECT,
        USER,
        PROJECT,
        VERSION,
        ISSUE,
        CUSTOM
    }
}