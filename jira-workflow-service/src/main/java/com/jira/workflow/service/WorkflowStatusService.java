package com.jira.workflow.service;

import com.jira.workflow.dto.CreateWorkflowStatusRequest;
import com.jira.workflow.dto.WorkflowStatusResponse;
import com.jira.workflow.entity.WorkflowStatus;
import com.jira.workflow.exception.DuplicateResourceException;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.WorkflowRepository;
import com.jira.workflow.repository.WorkflowStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowStatusService {

    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusCatalog statusCatalog;

    @Transactional(readOnly = true)
    public List<WorkflowStatusResponse> getWorkflowStatuses(UUID workflowId) {
        log.debug("Fetching statuses for workflow: {}", workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = statusCatalog.loadCatalog();
        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId);

        return statuses.stream()
                .map(ws -> mapToResponse(ws, catalog))
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowStatusResponse addStatusToWorkflow(UUID workflowId, CreateWorkflowStatusRequest request) {
        log.info("Adding status {} to workflow: {}", request.getStatusId(), workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        if (workflowStatusRepository.existsByWorkflowIdAndStatusId(workflowId, request.getStatusId())) {
            throw new DuplicateResourceException("Status already exists in workflow");
        }

        WorkflowStatus workflowStatus = WorkflowStatus.builder()
                .workflowId(workflowId)
                .statusId(request.getStatusId())
                .sequence(request.getSequence() != null ? request.getSequence() : 0)
                .build();

        workflowStatus = workflowStatusRepository.save(workflowStatus);
        return mapToResponse(workflowStatus, statusCatalog.loadCatalog());
    }

    @Transactional
    public void removeStatusFromWorkflow(UUID workflowId, UUID workflowStatusId) {
        WorkflowStatus workflowStatus = workflowStatusRepository.findById(workflowStatusId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowStatus", "id", workflowStatusId));

        if (!workflowStatus.getWorkflowId().equals(workflowId)) {
            throw new IllegalArgumentException("Status does not belong to this workflow");
        }

        workflowStatusRepository.delete(workflowStatus);
    }

    @Transactional
    public List<WorkflowStatusResponse> reorderWorkflowStatuses(UUID workflowId, List<UUID> statusIds) {
        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId);
        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = statusCatalog.loadCatalog();

        for (int i = 0; i < statusIds.size(); i++) {
            UUID statusId = statusIds.get(i);
            final int sequence = i;
            statuses.stream()
                    .filter(s -> s.getStatusId().equals(statusId))
                    .findFirst()
                    .ifPresent(s -> s.setSequence(sequence));
        }

        workflowStatusRepository.saveAll(statuses);
        return workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId)
                .stream()
                .map(ws -> mapToResponse(ws, catalog))
                .collect(Collectors.toList());
    }

    private WorkflowStatusResponse mapToResponse(WorkflowStatus workflowStatus, Map<String, WorkflowStatusCatalog.StatusMeta> catalog) {
        WorkflowStatusCatalog.StatusMeta meta = statusCatalog.resolve(workflowStatus.getStatusId(), catalog);

        return WorkflowStatusResponse.builder()
                .id(workflowStatus.getId())
                .workflowId(workflowStatus.getWorkflowId())
                .statusId(workflowStatus.getStatusId())
                .statusName(meta.name())
                .statusCategory(meta.category())
                .statusColor(meta.color())
                .sequence(workflowStatus.getSequence())
                .createdAt(workflowStatus.getCreatedAt())
                .build();
    }
}
