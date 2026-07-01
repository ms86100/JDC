package com.jira.document.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_holds", schema = "jira_document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalHold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "legal_matter_id")
    private UUID legalMatterId;

    @Column(name = "matter_reference", length = 100)
    private String matterReference;

    @Column(name = "hold_type", nullable = false, length = 100)
    private String holdType;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @Column(name = "custodian_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] custodianIds = new UUID[]{};

    @Column(name = "custodian_names", columnDefinition = "text[]")
    @Builder.Default
    private String[] custodianNames = new String[]{};

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "auto_extend", nullable = false)
    @Builder.Default
    private Boolean autoExtend = false;

    @Column(name = "extension_period_days")
    @Builder.Default
    private Integer extensionPeriodDays = 30;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Column(columnDefinition = "TEXT")
    private String preservationInstructions;

    @Column(name = "data_categories", columnDefinition = "text[]")
    @Builder.Default
    private String[] dataCategories = new String[]{};

    @Column(name = "project_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] projectIds = new UUID[]{};

    @Column(name = "legal_basis", length = 255)
    private String legalBasis;

    @Column(name = "is_critical", nullable = false)
    @Builder.Default
    private Boolean isCritical = false;

    @Column(name = "notification_sent", nullable = false)
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "last_notification_at")
    private LocalDateTime lastNotificationAt;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Column(columnDefinition = "TEXT")
    private String releaseReason;
}
