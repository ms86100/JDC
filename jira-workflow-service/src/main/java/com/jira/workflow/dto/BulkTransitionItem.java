package com.jira.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class BulkTransitionItem {
    @NotNull
    private UUID issueId;
    private UUID transitionId;
    private UUID statusId;
    private String comment;
    private UUID resolutionId;
    private Map<String, Object> screenInput;
}
