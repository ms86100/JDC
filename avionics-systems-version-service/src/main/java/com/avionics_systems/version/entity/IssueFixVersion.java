package com.avionics_systems.version.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_fix_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueFixVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}