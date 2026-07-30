package com.avionics_systems.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpsertScreenIssueTypeOverrideRequest {
    @NotNull
    private UUID issueTypeId;

    @NotBlank
    private String screenType;

    @NotNull
    private UUID screenId;
}
