package com.avionics_systems.migration.service;

import com.avionics_systems.migration.service.clients.dto.CreateIssueRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Maps migration CreateIssueRequest to issue-service JSON (title, UUID projectId, resolved refs).
 */
@Component
@RequiredArgsConstructor
public class IssueServicePayloadMapper {

    private final IssueReferenceResolver issueReferenceResolver;

    public Map<String, Object> toIssueServicePayload(CreateIssueRequest request) {
        Map<String, Object> payload = new HashMap<>();
        String summary = request.getSummary();
        payload.put("title", summary);
        payload.put("summary", summary);
        payload.put("description", request.getDescription());

        if (request.getProjectId() != null) {
            payload.put("projectId", parseUuid(request.getProjectId()));
        }

        String issueTypeName = request.getIssueType();
        if (issueTypeName != null && !issueTypeName.isBlank()) {
            UUID typeId = issueReferenceResolver.resolveIssueTypeId(issueTypeName);
            if (typeId != null) {
                payload.put("issueTypeId", typeId);
            }
        }

        String priorityName = request.getPriority();
        if (priorityName != null && !priorityName.isBlank()) {
            UUID priorityId = issueReferenceResolver.resolvePriorityId(priorityName);
            if (priorityId != null) {
                payload.put("priorityId", priorityId);
            }
        }

        UUID assignee = parseUuid(request.getAssigneeId());
        if (assignee != null) {
            payload.put("assigneeId", assignee);
        }

        UUID reporter = parseUuid(request.getReporterId());
        if (reporter != null) {
            payload.put("reporterId", reporter);
        }

        UUID parent = parseUuid(request.getParentId());
        if (parent != null) {
            payload.put("parentIssueId", parent);
        }

        UUID epic = parseUuid(request.getEpicId());
        if (epic != null) {
            payload.put("epicId", epic);
        }

        if (request.getStoryPoints() != null) {
            payload.put("storyPoints", request.getStoryPoints().intValue());
        }

        if (request.getDueDate() != null) {
            payload.put("dueDate", request.getDueDate().toLocalDate());
        }

        if (request.getOriginalEstimate() != null) {
            payload.put("originalEstimate", request.getOriginalEstimate());
        }
        if (request.getRemainingEstimate() != null) {
            payload.put("remainingEstimate", request.getRemainingEstimate());
        }
        if (request.getTimeSpent() != null) {
            payload.put("timeSpent", request.getTimeSpent());
        }

        if (request.getOriginalIssueKey() != null && !request.getOriginalIssueKey().isBlank()) {
            payload.put("issueKey", request.getOriginalIssueKey());
            payload.put("originalIssueKey", request.getOriginalIssueKey());
            payload.put("migrationSourceKey", request.getOriginalIssueKey());
        }
        if (request.getMigrationCreatedAt() != null) {
            payload.put("migrationCreatedAt", request.getMigrationCreatedAt().toString());
        }
        if (request.getMigrationUpdatedAt() != null) {
            payload.put("migrationUpdatedAt", request.getMigrationUpdatedAt().toString());
        }
        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
            payload.put("labels", request.getLabels().toArray(new String[0]));
        }
        if (request.getComponents() != null && !request.getComponents().isEmpty()) {
            List<UUID> componentIds = new ArrayList<>();
            for (String c : request.getComponents()) {
                UUID id = parseUuid(c);
                if (id != null) {
                    componentIds.add(id);
                }
            }
            if (!componentIds.isEmpty()) {
                payload.put("componentIds", componentIds.toArray(new UUID[0]));
            }
        }

        return payload;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
