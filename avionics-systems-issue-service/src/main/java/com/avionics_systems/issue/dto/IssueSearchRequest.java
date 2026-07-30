package com.avionics_systems.issue.dto;

import lombok.*;

import java.util.UUID;

/**
 * Enhanced Issue Search Request with comprehensive filter options.
 * Supports filtering by multiple criteria including full-text search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSearchRequest {

    // Basic filters
    private UUID projectId;
    private UUID status;
    private UUID assigneeId;
    private UUID reporterId;
    private UUID priorityId;
    private UUID issueTypeId;

    // Text search
    private String text;           // Search in title and description
    private String titleContains;  // Search in title only
    private String descriptionContains; // Search in description only

    // Epic filtering
    private UUID epicId;

    // Parent filtering (for subtask queries)
    private UUID parentIssueId;
    private Boolean hasParent;      // Filter issues with/without parents
    private Boolean isSubtask;     // Filter subtasks only

    // Version filtering
    private UUID affectsVersionId;
    private UUID fixVersionId;

    // Component filtering
    private UUID componentId;

    // Label filtering
    private String label;

    // Security filtering
    private UUID securityLevelId;

    // Resolution filtering
    private UUID resolutionId;
    private Boolean unresolved;  // Filter issues without resolution

    // Date range filtering
    private String createdAfter;
    private String createdBefore;
    private String updatedAfter;
    private String updatedBefore;
    private String dueDateAfter;
    private String dueDateBefore;

    // Story points filtering
    private Integer minStoryPoints;
    private Integer maxStoryPoints;

    // Pagination
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    // Sorting
    private String sortBy;     // Field to sort by (default: createdAt)
    private String sortOrder;   // ASC or DESC (default: DESC)

    // Additional options
    private Boolean includeArchived;
}