package com.avionics_systems.plan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanItemRequest {

    @NotNull(message = "Issue ID is required")
    private UUID issueId;

    private String issueKey;

    private String issueTitle;

    @NotBlank(message = "Issue type is required")
    private String issueType;

    private UUID parentId;

    private String sortOrder;

    private LocalDate targetDate;

    private LocalDate targetEndDate;

    private String status;

    private String statusCategory;

    private Integer storyPoints;

    private UUID assigneeId;
}