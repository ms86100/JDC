package com.avionics_systems.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Status Definition - System-wide status definitions with colors and categories
 */
@Entity
@Table(name = "status_definitions", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "status_key", nullable = false, unique = true, length = 20)
    private String statusKey;

    @Column(name = "status_name", nullable = false, length = 50)
    private String statusName;

    @Column(name = "status_color", nullable = false, length = 7)
    private String statusColor;

    @Column(name = "status_icon", length = 50)
    private String statusIcon;

    @Column(name = "status_category", nullable = false, length = 20)
    private String statusCategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Status categories
    public static final String CATEGORY_TODO = "TODO";
    public static final String CATEGORY_IN_PROGRESS = "IN_PROGRESS";
    public static final String CATEGORY_DONE = "DONE";
}