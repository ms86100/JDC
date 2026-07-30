package com.avionics_systems.sprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.board.dto.BoardIssueResponse;
import com.avionics_systems.cluster.util.StatusCategoryHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTTP client for avionics-systems-issue-service — board issue loading, status, rank.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IssueServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageSource messageSource;

    @Value("${avionics-systems.issue-service-url:http://localhost:8084}")
    private String issueServiceUrl;

    public List<BoardIssueResponse> fetchBoardIssues(UUID projectId, String extraJql) {
        List<Map<String, Object>> raw = new ArrayList<>();
        try {
            if (extraJql != null && !extraJql.isBlank()) {
                String jql = appendProjectScope(extraJql, projectId);
                raw = searchJql(jql);
            } else {
                raw = listByProject(projectId, 0, 500);
            }
        } catch (Exception e) {
            log.warn("Issue fetch failed for project {}: {}", projectId, e.getMessage());
        }
        return raw.stream().map(this::mapToBoardIssue).collect(Collectors.toList());
    }

    public BoardIssueResponse moveIssueStatus(UUID issueId, UUID projectId, String targetStatusName, String rank) {
        UUID statusId = resolveStatusIdByName(targetStatusName);
        if (statusId != null) {
            patchStatus(issueId, projectId, statusId);
        }
        if (rank != null && !rank.isBlank()) {
            patchRank(issueId, rank);
        }
        Map<String, Object> issue = getIssueRaw(issueId);
        return issue != null ? mapToBoardIssue(issue) : BoardIssueResponse.builder().id(issueId).status(targetStatusName).rank(rank).build();
    }

    public void reorderIssueRank(UUID issueId, String rank) {
        patchRank(issueId, rank);
    }

    public BoardIssueResponse getBoardIssue(UUID issueId) {
        Map<String, Object> raw = getIssueRaw(issueId);
        return raw != null ? mapToBoardIssue(raw) : null;
    }

    // --- legacy sprint helpers ---

    public IssueData getIssue(UUID issueId) {
        Map<String, Object> response = getIssueRaw(issueId);
        return response != null ? mapToIssueData(response) : new IssueData();
    }

    public List<IssueData> getIssues(List<UUID> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) return Collections.emptyList();
        try {
            String ids = issueIds.stream().map(UUID::toString).collect(Collectors.joining(","));
            String url = issueServiceUrl + "/api/issues/batch?ids=" + ids;
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            if (response.getBody() != null) {
                return response.getBody().stream().map(this::mapToIssueData).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch issues batch: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    public int calculateSprintPoints(List<UUID> issueIds) {
        return getIssues(issueIds).stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();
    }

    public int calculateCompletedPoints(List<UUID> issueIds) {
        return getIssues(issueIds).stream()
                .filter(i -> isCompletedStatus(i.getStatusName()))
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();
    }

    // --- bulk-operation helpers ---

    /**
     * Delete an issue via the issue service REST API.
     */
    public void deleteIssue(UUID issueId) {
        String url = issueServiceUrl + "/api/issues/" + issueId;
        HttpHeaders headers = new HttpHeaders();
        // Service-to-service call: use a system user ID
        headers.set("X-User-Id", "00000000-0000-0000-0000-000000000000");
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            log.info("Deleted issue {} via issue-service", issueId);
        } catch (Exception e) {
            throw new RuntimeException(messageSource.getMessage("error.issue.delete.failed", new Object[]{issueId, e.getMessage()}, java.util.Locale.ENGLISH), e);
        }
    }

    /**
     * Update issue fields (assignee, priority, labels) via PUT to the issue service.
     */
    public void updateIssueFields(UUID issueId, Map<String, Object> fields) {
        String url = issueServiceUrl + "/api/issues/" + issueId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", "00000000-0000-0000-0000-000000000000");
        try {
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(fields, headers), Map.class);
            log.info("Updated fields on issue {}: {}", issueId, fields.keySet());
        } catch (Exception e) {
            throw new RuntimeException(messageSource.getMessage("error.issue.update.failed", new Object[]{issueId, e.getMessage()}, java.util.Locale.ENGLISH), e);
        }
    }

    /**
     * Update issue status via the issue service workflow endpoint.
     */
    public void updateIssueStatus(UUID issueId, UUID projectId, String statusName) {
        UUID statusId = resolveStatusIdByName(statusName);
        if (statusId == null) {
            throw new RuntimeException(messageSource.getMessage("error.issue.status.resolve.failed", new Object[]{statusName}, java.util.Locale.ENGLISH));
        }
        patchStatus(issueId, projectId, statusId);
        log.info("Updated status on issue {} to {}", issueId, statusName);
    }

    /**
     * Clone an issue via the issue service REST API.
     * Returns the cloned issue key or throws on failure.
     */
    public String cloneIssue(UUID issueId, UUID targetProjectId, boolean keepAttachments) {
        try {
            String url;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", "00000000-0000-0000-0000-000000000000");
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map<String, Object>> response;
            if (targetProjectId != null) {
                url = issueServiceUrl + "/api/issues/" + issueId + "/clone-to-project?targetProjectId=" + targetProjectId;
                response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {});
            } else {
                url = issueServiceUrl + "/api/issues/" + issueId + "/clone?includeAttachments=" + keepAttachments;
                response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {});
            }

            Map<String, Object> body = response.getBody();
            if (body != null) {
                // Try to get the clone's issue key from the response
                Object cloneKey = body.get("issueKey");
                if (cloneKey == null) cloneKey = body.get("key");
                if (cloneKey == null) cloneKey = body.get("id");
                return cloneKey != null ? cloneKey.toString() : "cloned";
            }
            return "cloned";
        } catch (Exception e) {
            throw new RuntimeException(messageSource.getMessage("error.issue.clone.failed", new Object[]{issueId, e.getMessage()}, java.util.Locale.ENGLISH), e);
        }
    }

    // --- internals ---

    private List<Map<String, Object>> listByProject(UUID projectId, int page, int size) {
        String url = issueServiceUrl + "/api/issues?projectId=" + projectId + "&page=" + page + "&size=" + size;
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        return extractContent(response.getBody());
    }

    private List<Map<String, Object>> searchJql(String jql) {
        String url = issueServiceUrl + "/api/issues/search?jql=" + java.net.URLEncoder.encode(jql, java.nio.charset.StandardCharsets.UTF_8) + "&pageSize=500";
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        Map<String, Object> body = response.getBody();
        if (body == null) return Collections.emptyList();
        Object issues = body.get("issues");
        if (issues instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cast = (List<Map<String, Object>>) list;
            return cast;
        }
        return extractContent(body);
    }

    private String appendProjectScope(String jql, UUID projectId) {
        String scope = "projectId = \"" + projectId + "\"";
        if (jql == null || jql.isBlank()) return scope;
        return scope + " AND (" + jql + ")";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContent(Map<String, Object> page) {
        if (page == null) return Collections.emptyList();
        Object content = page.get("content");
        if (content instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }

    private Map<String, Object> getIssueRaw(UUID issueId) {
        try {
            String url = issueServiceUrl + "/api/issues/" + issueId;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            return response.getBody();
        } catch (Exception e) {
            log.warn("Failed to fetch issue {}: {}", issueId, e.getMessage());
            return null;
        }
    }

    private void patchStatus(UUID issueId, UUID projectId, UUID statusId) {
        String url = issueServiceUrl + "/api/issues/" + issueId + "/status?projectId=" + projectId;
        Map<String, Object> body = Map.of("statusId", statusId.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.warn("Status patch failed for {}: {}", issueId, e.getMessage());
        }
    }

    private void patchRank(UUID issueId, String rank) {
        String url = issueServiceUrl + "/api/issues/" + issueId;
        Map<String, Object> body = Map.of("rank", rank);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.warn("Rank update failed for {}: {}", issueId, e.getMessage());
        }
    }

    private UUID resolveStatusIdByName(String statusName) {
        if (statusName == null) return null;
        try {
            String url = issueServiceUrl + "/api/issues/statuses";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            if (response.getBody() == null) return null;
            String norm = normalize(statusName);
            for (Map<String, Object> s : response.getBody()) {
                String name = String.valueOf(s.getOrDefault("name", ""));
                if (normalize(name).equals(norm) || normalize(name).contains(norm) || norm.contains(normalize(name))) {
                    return parseUUID(s.get("id"));
                }
            }
        } catch (Exception e) {
            log.warn("Status lookup failed: {}", e.getMessage());
        }
        return null;
    }

    private BoardIssueResponse mapToBoardIssue(Map<String, Object> response) {
        LocalDateTime created = parseDateTime(response.get("createdAt"));
        LocalDateTime updated = parseDateTime(response.get("updatedAt"));

        @SuppressWarnings("unchecked")
        List<String> labels = response.get("labels") instanceof List<?> l
                ? (List<String>) l
                : Collections.emptyList();

        return BoardIssueResponse.builder()
                .id(parseUUID(response.get("id")))
                .issueKey(stringVal(response.get("issueKey"), response.get("key")))
                .title(stringVal(response.get("title"), response.get("summary")))
                .status(stringVal(response.get("status"), response.get("statusName"), extractNestedName(response.get("status"))))
                .priority(stringVal(response.get("priority"), response.get("priorityName"), extractNestedName(response.get("priority"))))
                .issueType(stringVal(response.get("issueType"), response.get("issueTypeName"), extractNestedName(response.get("issueType"))))
                .assigneeId(parseUUID(response.get("assigneeId")))
                .assigneeName(stringVal(response.get("assigneeName")))
                .reporterId(parseUUID(response.get("reporterId")))
                .epicId(parseUUID(response.get("epicId")))
                .epicName(stringVal(response.get("epicName")))
                .epicColor(stringVal(response.get("epicColor")))
                .storyPoints(intVal(response.get("storyPoints")))
                .labels(labels)
                .created(created)
                .updated(updated)
                .sprintId(parseUUID(response.get("sprintId")))
                .sprintName(stringVal(response.get("sprintName")))
                .dueDate(stringVal(response.get("dueDate")))
                .rank(stringVal(response.get("rank")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private IssueData mapToIssueData(Map<String, Object> response) {
        IssueData data = new IssueData();
        data.setId(parseUUID(response.get("id")));
        data.setProjectId(parseUUID(response.get("projectId")));
        data.setIssueKey(stringVal(response.get("issueKey"), response.get("key")));
        data.setTitle(stringVal(response.get("title"), ""));
        data.setStoryPoints(intVal(response.get("storyPoints")));
        data.setStatusName(stringVal(response.get("status"), response.get("statusName"), extractNestedName(response.get("status"))));
        data.setPriorityName(stringVal(response.get("priority"), response.get("priorityName"), extractNestedName(response.get("priority"))));
        data.setIssueTypeName(stringVal(response.get("issueType"), response.get("issueTypeName"), extractNestedName(response.get("issueType"))));
        data.setAssigneeId(parseUUID(response.get("assigneeId")));
        data.setAssigneeName(stringVal(response.get("assigneeName")));
        data.setReporterId(parseUUID(response.get("reporterId")));
        return data;
    }

    private String extractNestedName(Object statusObj) {
        if (statusObj instanceof Map<?, ?> m) {
            Object name = m.get("name");
            return name != null ? name.toString() : null;
        }
        return statusObj instanceof String s ? s : null;
    }

    private String stringVal(Object... vals) {
        for (Object v : vals) {
            if (v != null && !v.toString().isBlank()) return v.toString();
        }
        return null;
    }

    private Integer intVal(Object o) {
        if (o instanceof Number n) return n.intValue();
        return null;
    }

    private LocalDateTime parseDateTime(Object o) {
        if (o == null) return null;
        try {
            return LocalDateTime.parse(o.toString(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(o.toString().replace("Z", ""));
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private UUID parseUUID(Object obj) {
        if (obj == null) return null;
        if (obj instanceof UUID u) return u;
        try {
            return UUID.fromString(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        // Remove (legacy), (new), etc. for comparison
        return s.toLowerCase().replace("(legacy)", "").replace("(new)", "").replaceAll("[\\s_\\-()]+", "");
    }

    private boolean isCompletedStatus(String status) {
        return StatusCategoryHelper.isCompleted(status);
    }

    @Data
    public static class IssueData {
        private UUID id;
        private UUID projectId;
        private String issueKey;
        private String title;
        private Integer storyPoints;
        private Integer businessValue;
        private Long originalEstimate;
        private String statusName;
        private String statusCategory;
        private String priorityName;
        private String issueTypeName;
        private UUID assigneeId;
        private String assigneeName;
        private UUID reporterId;
        private Date createdAt;
        private Date updatedAt;
    }
}
