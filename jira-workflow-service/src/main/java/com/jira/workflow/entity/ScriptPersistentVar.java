package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "script_persistent_vars", schema = "jira_workflow",
        uniqueConstraints = @UniqueConstraint(columnNames = {"var_key", "scope", "scope_id"}))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptPersistentVar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "var_key", nullable = false)
    private String varKey;

    @Column(name = "var_value", columnDefinition = "TEXT")
    private String varValue;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String scope = "GLOBAL";

    @Column(name = "scope_id")
    private UUID scopeId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
