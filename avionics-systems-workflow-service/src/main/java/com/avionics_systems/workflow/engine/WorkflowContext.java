package com.avionics_systems.workflow.engine;

import com.avionics_systems.workflow.entity.Workflow;
import com.avionics_systems.workflow.entity.WorkflowTransition;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class WorkflowContext {
    private UUID issueId;
    private UUID projectId;
    private UUID issueTypeId;
    private UUID currentStatusId;
    private UUID userId;
    private Workflow workflow;
    private WorkflowTransition transition;
    private Map<String, Object> issueData;
    private Map<String, Object> userData;
    private Map<String, Object> screenInput;
    private String comment;
    private UUID resolutionId;
}
