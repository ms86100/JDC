package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for test plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPlanResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String testType;
    private List<String> labels;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String targetVersion;
    private String environment;
    private UUID ownerId;
    private List<UUID> testSetIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}