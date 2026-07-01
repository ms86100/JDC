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

        // Load catalog ONCE and share between services to avoid redundant HTTP calls
        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = statusCatalog.loadCatalog();

        WorkflowResponse workflow = workflowService.getWorkflow(workflowId);
        List<WorkflowStatusResponse> statuses = workflowStatusService.getWorkflowStatuses(workflowId, catalog);

        // Use bulk fetching via getTransitionsWithDetails to avoid N+1 queries
        List<TransitionDetailResponse> transitions = workflowService.getTransitionsWithDetails(workflowId).stream()
                .map(t -> {
                    WorkflowStatusCatalog.StatusMeta from = statusCatalog.resolve(t.getFromStatusId(), catalog);
                    WorkflowStatusCatalog.StatusMeta to = statusCatalog.resolve(t.getToStatusId(), catalog);
                    t.setFromStatusName(from.name());
                    t.setToStatusName(to.name());
                    t.setFromStatusCategory(from.category());
                    t.setToStatusCategory(to.category());
                    t.setFromStatusColor(from.color());
                    t.setToStatusColor(to.color());
                    return t;
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
