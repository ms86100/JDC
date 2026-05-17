package com.jira.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVersionRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Version name is required")
    private String name;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime releaseDate;

    private String semanticVersion;

    private String buildNumber;

    private String branchName;

    private String releaseTrain;

    private String color;
}