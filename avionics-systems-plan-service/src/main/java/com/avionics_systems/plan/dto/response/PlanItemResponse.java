package com.avionics_systems.plan.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanItemResponse {

    private UUID id;
    private UUID planId;
    private UUID issueId;
    private String issueKey;
    private String issueType;
    private String summary;
    private String status;
    private UUID parentId;
    private String parentKey;
    private String sortOrder;
    private LocalDate targetDate;
    private LocalDate targetEndDate;
    private String assigneeId;
    private String assigneeName;
    private Integer storyPoints;
    private Integer childCount;
    private Double progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
