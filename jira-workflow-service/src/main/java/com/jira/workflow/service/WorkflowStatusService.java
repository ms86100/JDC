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
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ADMIN_SERVICE_URL = "http://localhost:8093";

    @Transactional(readOnly = true)
    public List<WorkflowStatusResponse> getWorkflowStatuses(UUID workflowId) {
        log.debug("Fetching statuses for workflow: {}", workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId);

        return statuses.stream()
                .map(this::mapToResponse)
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
        log.info("Status added to workflow: {}", workflowStatus.getId());

        return mapToResponse(workflowStatus);
    }

    @Transactional
    public void removeStatusFromWorkflow(UUID workflowId, UUID workflowStatusId) {
        log.info("Removing status {} from workflow: {}", workflowStatusId, workflowId);

        WorkflowStatus workflowStatus = workflowStatusRepository.findById(workflowStatusId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowStatus", "id", workflowStatusId));

        if (!workflowStatus.getWorkflowId().equals(workflowId)) {
            throw new IllegalArgumentException("Status does not belong to this workflow");
        }

        workflowStatusRepository.delete(workflowStatus);
        log.info("Status removed from workflow: {}", workflowStatusId);
    }

    @Transactional
    public List<WorkflowStatusResponse> reorderWorkflowStatuses(UUID workflowId, List<UUID> statusIds) {
        log.info("Reordering statuses for workflow: {}", workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId);

        for (int i = 0; i < statusIds.size(); i++) {
            UUID statusId = statusIds.get(i);
            final int sequence = i;
            statuses.stream()
                    .filter(s -> s.getStatusId().equals(statusId))
                    .findFirst()
                    .ifPresent(s -> s.setSequence(sequence));
        }

        workflowStatusRepository.saveAll(statuses);
        log.info("Statuses reordered for workflow: {}", workflowId);

        return workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WorkflowStatusResponse mapToResponse(WorkflowStatus workflowStatus) {
        String statusName = null;
        String statusCategory = null;
        String statusColor = null;

        try {
            String url = ADMIN_SERVICE_URL + "/api/admin/statuses/" + workflowStatus.getStatusId();
            Map<String, Object> statusData = restTemplate.getForObject(url, Map.class);
            if (statusData != null) {
                statusName = (String) statusData.get("name");
                statusCategory = (String) statusData.get("statusCategory");
                statusColor = (String) statusData.get("statusColor");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch status details for {}: {}", workflowStatus.getStatusId(), e.getMessage());
        }

        return WorkflowStatusResponse.builder()
                .id(workflowStatus.getId())
                .workflowId(workflowStatus.getWorkflowId())
                .statusId(workflowStatus.getStatusId())
                .statusName(statusName)
                .statusCategory(statusCategory)
                .statusColor(statusColor)
                .sequence(workflowStatus.getSequence())
                .createdAt(workflowStatus.getCreatedAt())
                .build();
    }
}