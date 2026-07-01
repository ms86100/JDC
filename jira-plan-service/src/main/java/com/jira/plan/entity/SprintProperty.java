package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sprint_properties", schema = "jira_plan",
    uniqueConstraints = @UniqueConstraint(columnNames = {"sprint_id", "property_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "property_key", nullable = false, length = 255)
    private String propertyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "property_value", columnDefinition = "jsonb")
    private String propertyValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
