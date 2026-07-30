package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

/**
 * Request DTO for indexing an entity in the search service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexEntityRequest {

    private String entityType;
    private String entityId;
    private String title;
    private String content;
    private String projectId;
    private String issueType;
    private String status;
    private String assigneeId;
    private String reporterId;
    private String[] labels;
    private String[] components;
    private String createdFrom;
    private String createdTo;
    private String updatedFrom;
    private String updatedTo;
}