package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_levels", schema = "jira_project")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SecurityLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scheme_id")
    private UUID schemeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "level_type", length = 20)
    @Builder.Default
    private String levelType = "RESTRICTED";

    @Column
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}