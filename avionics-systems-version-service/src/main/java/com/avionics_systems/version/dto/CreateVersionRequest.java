package com.avionics_systems.version.dto;

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

    @NotNull(message = "{validation.version.projectId.required}")
    private UUID projectId;

    @NotBlank(message = "{validation.version.name.required}")
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