package com.avionics_systems.issue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RankIssueRequest {
    @NotNull
    private UUID projectId;
    /** UP or DOWN relative to other issues in the project */
    @NotNull
    private String direction;
}
