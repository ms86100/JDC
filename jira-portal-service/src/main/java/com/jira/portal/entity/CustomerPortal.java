package com.jira.portal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_portals", schema = "jira_portal",
    indexes = {
        @Index(name = "idx_portal_project_id", columnList = "project_id"),
        @Index(name = "idx_portal_status", columnList = "status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPortal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "portal_key", nullable = false, unique = true, length = 100)
    private String portalKey;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, PUBLISHED, ARCHIVED

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "require_authentication", nullable = false)
    @Builder.Default
    private Boolean requireAuthentication = true;

    @Column(name = "allow_anonymous_submissions", nullable = false)
    @Builder.Default
    private Boolean allowAnonymousSubmissions = false;

    @Column(columnDefinition = "TEXT")
    private String brandingConfig; // JSON branding configuration

    @Column(columnDefinition = "TEXT")
    private String layoutConfig; // JSON layout configuration

    @Column(columnDefinition = "TEXT")
    private String headerContent; // HTML for custom header

    @Column(columnDefinition = "TEXT")
    private String footerContent; // HTML for custom footer

    @Column(columnDefinition = "TEXT")
    private String homepageContent; // Content for portal homepage

    @Column(name = "request_type_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] requestTypeIds = new UUID[]{};

    @Column(columnDefinition = "TEXT")
    private String customCss;

    @Column(name = "google_analytics_key", length = 50)
    private String googleAnalyticsKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;
}