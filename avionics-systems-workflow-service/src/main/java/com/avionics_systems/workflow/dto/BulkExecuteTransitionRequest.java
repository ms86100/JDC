package com.avionics_systems.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkExecuteTransitionRequest {
    @NotNull
    private UUID projectId;
    private UUID userId;
    @NotEmpty
    @Valid
    private List<BulkTransitionItem> items;
}
