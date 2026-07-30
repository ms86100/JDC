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
 * Options for select-type custom fields.
 */
@Entity
@Table(name = "custom_field_options", schema = "jira_migration",
        uniqueConstraints = @UniqueConstraint(columnNames = {"custom_field_id", "parent_option_id", "value"}))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "custom_field_id", nullable = false)
    private UUID customFieldId;

    @Column(name = "parent_option_id")
    private UUID parentOptionId;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "sequence")
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "disabled")
    @Builder.Default
    private Boolean disabled = false;

    @Column(name = "cast_children")
    @Builder.Default
    private Boolean castChildren = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, Object> properties;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}