package com.jira.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDetailResponse {

    private WorkflowResponse workflow;

    @Builder.Default
    private List<WorkflowStatusResponse> statuses = new ArrayList<>();

    @Builder.Default
    private List<TransitionDetailResponse> transitions = new ArrayList<>();

    @Builder.Default
    private List<WorkflowVersionResponse> versions = new ArrayList<>();
}
