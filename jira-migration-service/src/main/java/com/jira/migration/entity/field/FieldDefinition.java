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
 * Metadata-driven field definition for the dynamic field architecture.
 * Supports plugin fields, custom fields, and future extensibility.
 */
@Entity
@Table(name = "field_definitions", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "field_key", nullable = false, unique = true, length = 255)
    private String fieldKey;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 50)
    private FieldType fieldType;

    @Enumerated(EnumType.STRING)
    @Column(name = "renderer", length = 100)
    @Builder.Default
    private FieldRenderer renderer = FieldRenderer.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_region", length = 50)
    @Builder.Default
    private ScreenRegion screenRegion = ScreenRegion.SIDEBAR;

    @Column(name = "plugin_source", length = 255)
    private String pluginSource;

    @Column(name = "plugin_namespace", length = 255)
    private String pluginNamespace;

    @Column(name = "searchable")
    @Builder.Default
    private Boolean searchable = true;

    @Column(name = "sortable")
    @Builder.Default
    private Boolean sortable = true;

    @Column(name = "filterable")
    @Builder.Default
    private Boolean filterable = true;

    @Column(name = "required")
    @Builder.Default
    private Boolean required = false;

    @Column(name = "read_only")
    @Builder.Default
    private Boolean readOnly = false;

    @Column(name = "hidden")
    @Builder.Default
    private Boolean hidden = false;

    @Column(name = "search_weight")
    @Builder.Default
    private Integer searchWeight = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_definition", columnDefinition = "jsonb")
    private Map<String, Object> schemaDefinition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visibility_rules", columnDefinition = "jsonb")
    private Map<String, Object> visibilityRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "renderer_config", columnDefinition = "jsonb")
    private Map<String, Object> rendererConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private Map<String, Object> validationRules;

    @Column(name = "options")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<FieldOption> options;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "custom")
    @Builder.Default
    private Boolean custom = false;

    @Column(name = "built_in")
    @Builder.Default
    private Boolean builtIn = false;

    @Column(name = "deprecated")
    @Builder.Default
    private Boolean deprecated = false;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    public enum FieldType {
        TEXT,
        TEXTAREA,
        RICHTEXT,
        NUMBER,
        DATE,
        DATETIME,
        TIME,
        SINGLE_SELECT,
        MULTI_SELECT,
        CHECKBOX,
        RADIO,
        USER,
        GROUP,
        PROJECT,
        ISSUE_TYPE,
        STATUS,
        PRIORITY,
        RESOLUTION,
        COMPONENT,
        VERSION,
        LABEL,
        SECURITY_LEVEL,
        URL,
        EMAIL,
        FLOAT,
        STORY_POINTS,
        CURRENCY,
        DURATION,
        SPRINT,
        EPIC,
        PARENT_ISSUE,
        SUBTASK,
        VOTES,
        WATCHERS,
        ATTACHMENT,
        COMMENT,
        WORKLOG,
        CUSTOM,
        UNKNOWN
    }

    public enum FieldRenderer {
        TEXT,
        TEXTAREA,
        RICHTEXT,
        SELECT,
        MULTI_SELECT,
        USER_PICKER,
        GROUP_PICKER,
        PROJECT_PICKER,
        DATETIME_PICKER,
        NUMBER,
        SLIDER,
        RADIO,
        CHECKBOX,
        LABEL_EDITOR,
        CURRENCY,
        DURATION,
        URL_LINK,
        EMAIL_LINK,
        SECURITY_LEVEL,
        SPRINT_SELECTOR,
        EPIC_LINK,
        VOTES,
        WATCHERS,
        ATTACHMENT_UPLOAD,
        READ_ONLY,
        CUSTOM
    }

    public enum ScreenRegion {
        HEADER,
        LEFT_PRIMARY,
        LEFT_DESCRIPTION,
        LEFT_ACTIVITY,
        SIDEBAR,
        SIDEBAR_PEOPLE,
        SIDEBAR_DETAILS,
        SIDEBAR_TIME,
        SIDEBAR_AGILE,
        SIDEBAR_DATES,
        SIDEBAR_VERSIONS,
        MODAL,
        POPOVER
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOption {
        private String value;
        private String label;
        private Integer order;
        private String color;
        private Boolean disabled;
    }
}