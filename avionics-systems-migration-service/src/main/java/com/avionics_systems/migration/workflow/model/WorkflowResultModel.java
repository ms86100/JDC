package com.avionics_systems.migration.workflow.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResultModel {
    private String type;
    private String oldStatus;
    private String status;
    private String targetStepId;
    @Builder.Default
    private List<WorkflowFunctionDescriptor> conditions = new ArrayList<>();
}
