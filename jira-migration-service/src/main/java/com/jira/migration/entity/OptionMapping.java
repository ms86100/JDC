package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "option_mappings", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "wizard_session_id")
    private UUID wizardSessionId;

    @Column(name = "source_field_key", nullable = false)
    private String sourceFieldKey;

    @Column(name = "source_option_value", nullable = false, length = 500)
    private String sourceOptionValue;

    @Column(name = "target_field_key", nullable = false)
    private String targetFieldKey;

    @Column(name = "target_option_value", nullable = false, length = 500)
    private String targetOptionValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
