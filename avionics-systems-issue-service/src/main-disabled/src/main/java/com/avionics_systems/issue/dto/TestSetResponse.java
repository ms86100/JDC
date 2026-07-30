package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for test set
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSetResponse {

    private UUID id;
    private UUID projectId;
    private UUID folderId;
    private String name;
    private String description;
    private String testType;
    private List<String> labels;
    private Integer testCount;
    private List<String> requirementKeys;
    private String status;
    private UUID ownerId;
    private Boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}