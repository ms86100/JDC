package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "script_definitions", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDefinition {

    public static final String TYPE_CONDITION = "CONDITION";
    public static final String TYPE_VALIDATOR = "VALIDATOR";
    public static final String TYPE_POST_FUNCTION = "POST_FUNCTION";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "script_type", nullable = false, length = 20)
    private String scriptType;

    @Column(name = "script_key", nullable = false, unique = true)
    private String scriptKey;

    @Column(name = "script_body", nullable = false, columnDefinition = "TEXT")
    private String scriptBody;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
