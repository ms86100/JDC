package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resolution - How an issue was resolved
 * Matches Jira DC's RESOLUTION table
 */
@Entity
@Table(name = "resolutions", schema = "jira_issue")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Standard resolutions matching Jira DC
    public static final String FIXED = "Fixed";
    public static final String WONT_FIX = "Won't Fix";
    public static final String DUPLICATE = "Duplicate";
    public static final String INCOMPLETE = "Incomplete";
    public static final String CANNOT_REPRODUCE = "Cannot Reproduce";
    public static final String DONE = "Done";
}
