package com.avionics_systems.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_mappings", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "source_identifier", nullable = false, length = 255)
    private String sourceIdentifier; // username, email, user_id from source

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType; // LEGACY_DC, EXTERNAL

    @Column(name = "target_user_id")
    private UUID targetUserId; // Mapped to existing user

    @Column(name = "target_username", length = 150)
    private String targetUsername;

    @Column(name = "target_email", length = 255)
    private String targetEmail;

    @Column(name = "mapping_type", nullable = false, length = 20)
    private String mappingType; // EXACT_MATCH, EMAIL_MATCH, CREATE_NEW, MANUAL

    @Column(name = "confidence_score", precision = 5)
    private Double confidenceScore; // 0-100 for fuzzy matches

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}