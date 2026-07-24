package com.jira.migration.entity.field;

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
 * Custom field definition for user-created fields.
 * Extends the base field definition with custom field specific features.
 */
@Entity
@Table(name = "custom_field_definitions", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(name = "searcher_key", length = 255)
    private String searcherKey;

    @Column(name = "renderer_key", length = 255)
    private String rendererKey;

    @Column(name = "unique_type")
    @Builder.Default
    private Boolean uniqueType = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_values", columnDefinition = "jsonb")
    private Map<String, Object> defaultValues;

    @Column(name = "field_key", unique = true, length = 255)
    private String fieldKey;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "searchable")
    @Builder.Default
    private Boolean searchable = true;

    @Column(name = "navigable")
    @Builder.Default
    private Boolean navigable = true;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "clause_names", columnDefinition = "varchar[]")
    @Builder.Default
    private String[] clauseNames = new String[]{};

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    public static final String TYPE_TEXT = "com.atlassian.jira.plugin.system.customfieldtypes:textfield";
    public static final String TYPE_TEXTAREA = "com.atlassian.jira.plugin.system.customfieldtypes:textarea";
    public static final String TYPE_DATEPICKER = "com.atlassian.jira.plugin.system.customfieldtypes:datepicker";
    public static final String TYPE_DATETIME = "com.atlassian.jira.plugin.system.customfieldtypes:datetime";
    public static final String TYPE_NUMBER = "com.atlassian.jira.plugin.system.customfieldtypes:number";
    public static final String TYPE_SELECT = "com.atlassian.jira.plugin.system.customfieldtypes:select";
    public static final String TYPE_MULTI_SELECT = "com.atlassian.jira.plugin.system.customfieldtypes:multiselect";
    public static final String TYPE_RADIO = "com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons";
    public static final String TYPE_CHECKBOX = "com.atlassian.jira.plugin.system.customfieldtypes:checkbox";
    public static final String TYPE_USERPICKER = "com.atlassian.jira.plugin.system.customfieldtypes:userpicker";
    public static final String TYPE_MULTI_USERPICKER = "com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker";
    public static final String TYPE_PROJECTPICKER = "com.atlassian.jira.plugin.system.customfieldtypes:projectpicker";
    public static final String TYPE_VERSIONPICKER = "com.atlassian.jira.plugin.system.customfieldtypes:versionpicker";
    public static final String TYPE_LABELS = "com.atlassian.jira.plugin.system.customfieldtypes:labels";
    public static final String TYPE_URL = "com.atlassian.jira.plugin.system.customfieldtypes:url";
    public static final String TYPE_EMAIL = "com.atlassian.jira.plugin.system.customfieldtypes:email";
    public static final String TYPE_CASCADING_SELECT = "com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect";
    public static final String TYPE_FLOAT = "com.atlassian.jira.plugin.system.customfieldtypes:float";
}