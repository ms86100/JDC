package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhooks", schema = "jira_admin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(length = 500)
    private String secret;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String events;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "jql_filter", columnDefinition = "TEXT")
    private String jqlFilter;

    @Column(name = "exclude_body")
    @Builder.Default
    private Boolean excludeBody = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
