package com.avionics_systems.portal.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_requests", schema = "jira_portal",
    indexes = {
        @Index(name = "idx_request_portal_id", columnList = "portal_id"),
        @Index(name = "idx_request_status", columnList = "status"),
        @Index(name = "idx_request_customer_email", columnList = "customer_email")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "portal_id", nullable = false)
    private UUID portalId;

    @Column(name = "request_type_id")
    private UUID requestTypeId;

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "request_key", nullable = false, unique = true, length = 50)
    private String requestKey;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(name = "customer_id")
    private UUID customerId; // For authenticated customers

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "SUBMITTED"; // SUBMITTED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, CLOSED

    @Column(name = "priority", length = 50)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "organization_name", length = 255)
    private String organizationName;

    @Column(columnDefinition = "TEXT")
    private String fields; // JSON for additional request fields

    @Column(columnDefinition = "TEXT")
    private String attachments; // JSON array of attachment references

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "firstResponse_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "sla_breached", nullable = false)
    @Builder.Default
    private Boolean slaBreached = false;

    @Column(columnDefinition = "TEXT")
    private String channel; // WEB, EMAIL, API, CHAT

    @Column(name = "satisfaction_rating")
    @Builder.Default
    private Integer satisfactionRating = 0;

    @Column(name = "satisfaction_comment", columnDefinition = "TEXT")
    private String satisfactionComment;
}