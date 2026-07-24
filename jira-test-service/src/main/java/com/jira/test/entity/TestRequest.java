package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_key", length = 20)
    private String issueKey;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "request_type", length = 20)
    private String requestType;

    @Column(length = 30)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "fix_version_id")
    private UUID fixVersionId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Builder.Default
    private Boolean frozen = false;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> labels = List.of();

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
