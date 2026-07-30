package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "custom_field")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "field_key", nullable = false, unique = true, length = 255)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 50)
    private FieldType fieldType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum FieldType {
        TEXT, NUMBER, DATE, DATETIME, SELECT, MULTI_SELECT, CHECKBOX, RADIO, TEXTAREA, LABEL, URL, EMAIL,
        USER_PICKER, USER_PICKER_MULTI, PROJECT_PICKER, VERSION_PICKER, VERSION_PICKER_MULTI, LABELS, CASCADING_SELECT
    }
}