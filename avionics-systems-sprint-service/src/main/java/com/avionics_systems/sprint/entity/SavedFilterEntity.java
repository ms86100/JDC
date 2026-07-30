package com.avionics_systems.sprint.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_filters", schema = "jira_sprint")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedFilterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String jql;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "is_shared")
    @Builder.Default
    private Boolean isShared = false;

    @Column(name = "is_favorite")
    @Builder.Default
    private Boolean favorite = false;

    @Column(name = "share_type", length = 32)
    @Builder.Default
    private String shareType = "PRIVATE";

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
