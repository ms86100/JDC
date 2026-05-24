package com.jira.document.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalHoldResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID legalMatterId;
    private String matterReference;
    private String holdType;
    private String status;
    private UUID initiatedBy;
    private UUID[] custodianIds;
    private String[] custodianNames;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean autoExtend;
    private Integer extensionPeriodDays;
    private String scope;
    private String preservationInstructions;
    private String[] dataCategories;
    private UUID[] projectIds;
    private String legalBasis;
    private Boolean isCritical;
    private Boolean notificationSent;
    private LocalDateTime lastNotificationAt;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime releasedAt;
    private UUID releasedBy;
    private String releaseReason;
}