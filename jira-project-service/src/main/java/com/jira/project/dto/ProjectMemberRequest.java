package com.jira.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Project role name is required")
    private String projectRoleName;
}