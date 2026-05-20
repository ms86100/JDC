package com.jira.workflow.service;

import com.jira.workflow.dto.*;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.WorkflowRepository;
import com.jira.workflow.repository.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowDetailService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowService workflowService;
    private final WorkflowStatusService workflowStatusService;
    private final WorkflowDraftService workflowDraftService;
    private final WorkflowStatusCatalog statusCatalog;

    @Transactional(readOnly = true)
    public WorkflowDetailResponse getWorkflowDetail(UUID workflowId) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        WorkflowResponse workflow = workflowService.getWorkflow(workflowId);
        List<WorkflowStatusResponse> statuses = workflowStatusService.getWorkflowStatuses(workflowId);
        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = statusCatalog.loadCatalog();

        List<TransitionDetailResponse> transitions = workflowTransitionRepository.findByWorkflowId(workflowId).stream()
                .map(t -> {
                    TransitionDetailResponse base = workflowService.mapTransitionDetail(t);
                    WorkflowStatusCatalog.StatusMeta from = statusCatalog.resolve(t.getFromStatusId(), catalog);
                    WorkflowStatusCatalog.StatusMeta to = statusCatalog.resolve(t.getToStatusId(), catalog);
                    base.setFromStatusName(from.name());
                    base.setToStatusName(to.name());
                    base.setFromStatusCategory(from.category());
                    base.setToStatusCategory(to.category());
                    base.setFromStatusColor(from.color());
                    base.setToStatusColor(to.color());
                    return base;
                })
                .collect(Collectors.toList());

        List<WorkflowVersionResponse> versions = workflowDraftService.getVersionHistory(workflowId);

        return WorkflowDetailResponse.builder()
                .workflow(workflow)
                .statuses(statuses)
                .transitions(transitions)
                .versions(versions)
                .build();
    }
}
