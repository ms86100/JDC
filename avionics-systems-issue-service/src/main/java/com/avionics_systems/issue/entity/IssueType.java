package com.avionics_systems.issue.entity;

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
@Table(name = "issue_types", schema = "jira_issue")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "issue_type_key", nullable = false, unique = true, length = 50)
    private String issueTypeKey;

    @Column(length = 100)
    private String icon;

    @Column(name = "color", length = 20)
    private String color;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_subtask", nullable = false)
    @Builder.Default
    private Boolean isSubtask = false;

    @Column(name = "sequence")
    @Builder.Default
    private Integer sequence = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}