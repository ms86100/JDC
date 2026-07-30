package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exclusion_rules", schema = "jira_plan")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExclusionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "operator", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Operator operator;

    @Column(name = "field_value", nullable = false, length = 500)
    private String fieldValue;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        IN,
        NOT_IN,
        IS_EMPTY,
        IS_NOT_EMPTY,
        GREATER_THAN,
        LESS_THAN
    }

    public String toJqlFragment() {
        String field = fieldName.toLowerCase();
        String value = fieldValue;

        return switch (operator) {
            case EQUALS -> field + " = \"" + value + "\"";
            case NOT_EQUALS -> field + " != \"" + value + "\"";
            case CONTAINS -> field + " ~ \"" + value + "\"";
            case NOT_CONTAINS -> field + " !~ \"" + value + "\"";
            case STARTS_WITH -> field + " ~ \"" + value + "*\"";
            case ENDS_WITH -> field + " ~ \"*" + value + "\"";
            case IN -> field + " in (" + value + ")";
            case NOT_IN -> field + " not in (" + value + ")";
            case IS_EMPTY -> field + " is empty";
            case IS_NOT_EMPTY -> field + " is not empty";
            case GREATER_THAN -> field + " > " + value;
            case LESS_THAN -> field + " < " + value;
        };
    }
}