package com.jira.workflow.engine;

import com.jira.workflow.entity.*;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.*;
import com.jira.workflow.service.WorkflowSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkflowContextResolver {

    private final WorkflowIntegrationClient integrationClient;
    private final WorkflowSchemeService workflowSchemeService;
    private final WorkflowRepository workflowRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    public WorkflowContext resolveForIssue(UUID issueId, UUID projectId, UUID userId) {
        Map<String, Object> issueData = integrationClient.fetchIssue(issueId);
        UUID resolvedProjectId = projectId != null ? projectId : parseUuid(issueData.get("projectId"));
        UUID issueTypeId = parseUuid(issueData.get("issueTypeId"));
        UUID currentStatusId = parseUuid(issueData.get("statusId"));
        if (currentStatusId == null) {
            throw new IllegalStateException("Issue has no current status");
        }
        Workflow workflow = resolveWorkflow(resolvedProjectId, issueTypeId);
        return WorkflowContext.builder()
                .issueId(issueId)
                .projectId(resolvedProjectId)
                .issueTypeId(issueTypeId)
                .currentStatusId(currentStatusId)
                .userId(userId)
                .workflow(workflow)
                .issueData(issueData)
                .userData(integrationClient.fetchUser(userId))
                .build();
    }

    public WorkflowContext resolve(UUID issueId, UUID projectId, UUID userId, UUID transitionId, UUID targetStatusId) {
        Map<String, Object> issueData = integrationClient.fetchIssue(issueId);
        UUID resolvedProjectId = projectId != null ? projectId : parseUuid(issueData.get("projectId"));
        UUID issueTypeId = parseUuid(issueData.get("issueTypeId"));
        UUID currentStatusId = parseUuid(issueData.get("statusId"));

        if (currentStatusId == null) {
            throw new IllegalStateException("Issue has no current status");
        }

        Workflow workflow = resolveWorkflow(resolvedProjectId, issueTypeId);
        WorkflowTransition transition = resolveTransition(workflow, transitionId, currentStatusId, targetStatusId);

        return WorkflowContext.builder()
                .issueId(issueId)
                .projectId(resolvedProjectId)
                .issueTypeId(issueTypeId)
                .currentStatusId(currentStatusId)
                .userId(userId)
                .workflow(workflow)
                .transition(transition)
                .issueData(issueData)
                .userData(integrationClient.fetchUser(userId))
                .build();
    }

    public Workflow resolveWorkflow(UUID projectId, UUID issueTypeId) {
        if (projectId != null) {
            Optional<UUID> fromScheme = workflowSchemeService.getWorkflowForIssueType(projectId, issueTypeId);
            if (fromScheme.isPresent()) {
                return workflowRepository.findById(fromScheme.get())
                        .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", fromScheme.get()));
            }
        }

        if (projectId != null) {
            return workflowRepository.findByProjectIdAndIsDefaultTrue(projectId)
                    .or(() -> workflowRepository.findByProjectId(projectId).stream().findFirst())
                    .orElseGet(this::globalDefaultWorkflow);
        }

        return globalDefaultWorkflow();
    }

    private Workflow globalDefaultWorkflow() {
        return workflowRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "name", "default"));
    }

    private WorkflowTransition resolveTransition(
            Workflow workflow,
            UUID transitionId,
            UUID currentStatusId,
            UUID targetStatusId) {

        if (transitionId != null) {
            WorkflowTransition t = workflowTransitionRepository.findById(transitionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transition", "id", transitionId));
            if (!t.getWorkflowId().equals(workflow.getId())) {
                throw new IllegalArgumentException("Transition does not belong to resolved workflow");
            }
            return t;
        }

        if (targetStatusId != null) {
            List<WorkflowTransition> matches = workflowTransitionRepository
                    .findByWorkflowIdAndFromStatusId(workflow.getId(), currentStatusId);
            return matches.stream()
                    .filter(t -> targetStatusId.equals(t.getToStatusId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No transition from current status to target status in workflow"));
        }

        throw new IllegalArgumentException("transitionId or statusId is required");
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
