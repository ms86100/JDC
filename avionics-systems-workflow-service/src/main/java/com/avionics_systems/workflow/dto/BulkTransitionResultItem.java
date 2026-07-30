package com.avionics_systems.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BulkTransitionResultItem {
    private UUID issueId;
    private boolean success;
    private UUID newStatusId;
    private String error;
    private List<String> errors;
}
