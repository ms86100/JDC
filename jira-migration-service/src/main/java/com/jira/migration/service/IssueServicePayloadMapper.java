package com.jira.migration.service;

import com.jira.migration.service.clients.dto.CreateIssueRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps migration CreateIssueRequest to issue-service JSON (title, UUID projectId) — P0 blocker.
 */
@Component
public class IssueServicePayloadMapper {

    public Map<String, Object> toIssueServicePayload(CreateIssueRequest request) {
        Map<String, Object> payload = new HashMap<>();
        String summary = request.getSummary();
        payload.put("title", summary);
        payload.put("summary", summary);
        if (request.getIssueType() != null && !request.getIssueType().isBlank()) {
            payload.put("issueType", request.getIssueType());
            payload.put("issueTypeName", request.getIssueType());
        }
        payload.put("description", request.getDescription());

        if (request.getProjectId() != null) {
            payload.put("projectId", parseUuid(request.getProjectId()));
        }

        UUID assignee = parseUuid(request.getAssigneeId());
        if (assignee != null) {
            payload.put("assigneeId", assignee);
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

        if (request.getOriginalIssueKey() != null && !request.getOriginalIssueKey().isBlank()) {
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
            payload.put("labels", request.getLabels());
        }
        if (request.getStatus() != null) {
            payload.put("status", request.getStatus());
        }
        if (request.getPriority() != null) {
            payload.put("priority", request.getPriority());
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
