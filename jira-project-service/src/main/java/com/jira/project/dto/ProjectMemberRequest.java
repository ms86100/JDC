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

    @NotNull(message = "{validation.user.id.required}")
    private UUID userId;

    @NotBlank(message = "{validation.role.name.required}")
    private String projectRoleName;
}