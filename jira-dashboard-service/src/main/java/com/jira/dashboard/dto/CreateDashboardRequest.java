package com.jira.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDashboardRequest {

    @NotBlank(message = "{validation.dashboard.name.required}")
    @Size(max = 255, message = "{validation.dashboard.name.size}")
    private String name;

    @Size(max = 1000, message = "{validation.dashboard.description.size}")
    private String description;

    private UUID projectId;

    private Boolean isShared = false;

    private String layout = "DEFAULT";

    private String config;
}