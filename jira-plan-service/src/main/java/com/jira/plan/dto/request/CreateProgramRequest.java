package com.jira.plan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProgramRequest {

    @NotBlank(message = "Program name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    private String accessType;

    private UUID ownerId;

    private UUID planId;

    /** Plans to link when creating the program (DC connected plans checkboxes). */
    private List<UUID> linkedPlanIds;
}