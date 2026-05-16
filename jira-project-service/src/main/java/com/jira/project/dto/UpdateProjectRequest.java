package com.jira.project.dto;

import lombok.*;
import jakarta.validation.constraints.Min;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    private String name;
    private String description;
    private UUID leadUserId;

    /**
     * Version for optimistic locking.
     * If provided, the update will fail with 409 Conflict if the entity
     * has been modified by another user since the version was read.
     */
    @Min(0)
    private Long version;
}