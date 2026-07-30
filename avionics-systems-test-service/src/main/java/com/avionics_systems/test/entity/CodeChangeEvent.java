package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "code_change_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeChangeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @Column(name = "author", length = 255)
    private String author;

    @Column(name = "changed_files", columnDefinition = "JSONB", nullable = false)
    private String changedFiles; // [{path, change_type, lines_changed}]

    @Column(name = "pr_id", length = 100)
    private String prId;

    @Column(name = "branch", length = 255)
    private String branch;

    @CreationTimestamp
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}