package com.jira.issue.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a test set
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestSetRequest {

    private UUID folderId;
    private String name;
    private String description;
    private String testType; // MANUAL, AUTOMATED, MIXED, BDD
    private List<String> labels;
    private List<String> requirementKeys;
    private UUID ownerId;
}