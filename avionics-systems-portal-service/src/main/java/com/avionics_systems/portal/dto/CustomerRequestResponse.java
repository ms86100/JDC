package com.avionics_systems.portal.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestResponse {

    private UUID id;
    private UUID portalId;
    private UUID requestTypeId;
    private UUID issueId;
    private String requestKey;
    private String summary;
    private String description;
    private String customerName;
    private String customerEmail;
    private UUID customerId;
    private String status;
    private String priority;
    private UUID assignedAgentId;
    private UUID organizationId;
    private String organizationName;
    private String fields;
    private String attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private Boolean slaBreached;
    private String channel;
    private Integer satisfactionRating;
    private String satisfactionComment;
}