package com.jira.workflow.engine;

import com.jira.workflow.entity.*;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.*;
import com.jira.workflow.service.WorkflowSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final ProjectPermissionClient projectPermissionClient;

    @Value("${app.workflow.context.project-permissions:BROWSE_PROJECTS,CREATE_ISSUES,EDIT_ISSUES,RESOLVE_ISSUES,DELETE_ISSUES,ASSIGN_ISSUES,ASSIGNABLE_USER,LINK_ISSUES}")
    private String projectPermissionsStr;

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
                .userData(enrichUserWithProjectPermissions(userId, resolvedProjectId, integrationClient.fetchUser(userId)))
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
                .userData(enrichUserWithProjectPermissions(userId, resolvedProjectId, integrationClient.fetchUser(userId)))
                .build();
    }

    private Map<String, Object> enrichUserWithProjectPermissions(
            UUID userId, UUID projectId, Map<String, Object> userData) {
        if (userId == null || projectId == null) {
            return userData;
        }
        List<String> projectPermissions = Arrays.asList(projectPermissionsStr.split(","));
        List<String> granted = new ArrayList<>();
        for (String perm : projectPermissions) {
            if (projectPermissionClient.hasPermission(userId, projectId, perm)) {
                granted.add(perm);
            }
        }
        userData.put("permissions", granted);
        return userData;
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
            var found = workflowTransitionRepository.findById(transitionId);
            if (found.isPresent()) {
                WorkflowTransition t = found.get();
                if (!t.getWorkflowId().equals(workflow.getId())) {
                    throw new IllegalArgumentException("Transition does not belong to resolved workflow");
                }
                return t;
            }
            if (targetStatusId == null) {
                throw new ResourceNotFoundException("Transition", "id", transitionId);
            }
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
