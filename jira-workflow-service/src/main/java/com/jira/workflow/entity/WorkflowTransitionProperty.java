package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_transition_properties", schema = "jira_workflow", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"transition_id", "property_key"})
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;

    @Column(name = "property_key", nullable = false, length = 100)
    private String propertyKey;

    @Column(name = "property_value", columnDefinition = "TEXT")
    private String propertyValue;

    @Column(name = "property_type", length = 50)
    @Builder.Default
    private String propertyType = "STRING";

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static final String TYPE_STRING = "STRING";
    public static final String TYPE_INTEGER = "INTEGER";
    public static final String TYPE_BOOLEAN = "BOOLEAN";
    public static final String TYPE_JSON = "JSON";
}