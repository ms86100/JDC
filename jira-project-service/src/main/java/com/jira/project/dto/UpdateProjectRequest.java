package com.jira.project.dto;

import lombok.*;
import jakarta.validation.constraints.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    @Size(min = 1, max = 200, message = "Project name must be between 1 and 200 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private UUID leadUserId;

    /**
     * Version for optimistic locking.
     * If provided, the update will fail with 409 Conflict if the entity
     * has been modified by another user since the version was read.
     */
    @Min(value = 0, message = "Version must be non-negative")
    private Long version;
}