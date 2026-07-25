package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
public class JdcSprintApi {

    private final RestTemplate restTemplate;
    private final String planServiceUrl;
    private final String issueServiceUrl;

    public JdcSprintApi(RestTemplate restTemplate, String planServiceUrl, String issueServiceUrl) {
        this.restTemplate = restTemplate;
        this.planServiceUrl = planServiceUrl;
        this.issueServiceUrl = issueServiceUrl;
    }

    @HostAccess.Export
    public Map<String, Object> getSprint(String sprintId) {
        try {
            if (sprintId == null) return Map.of();
            Map<?, ?> response = restTemplate.getForObject(
                    planServiceUrl + "/api/plans/sprints/" + sprintId, Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("getSprint failed for {}: {}", sprintId, e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public Map<String, Object> getActiveSprint(String boardId) {
        try {
            if (boardId == null) return Map.of();
            List<?> response = restTemplate.getForObject(
                    planServiceUrl + "/api/plans/boards/" + boardId + "/sprints?state=active",
                    List.class);
            if (response != null && !response.isEmpty()) {
                Object first = response.get(0);
                if (first instanceof Map<?, ?> m) return toStringMap(m);
            }
            return Map.of();
        } catch (Exception e) {
            log.warn("getActiveSprint failed for board {}: {}", boardId, e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllSprints(String boardId) {
        try {
            if (boardId == null) return List.of();
            List<?> response = restTemplate.getForObject(
                    planServiceUrl + "/api/plans/boards/" + boardId + "/sprints",
                    List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(toStringMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("getAllSprints failed for board {}: {}", boardId, e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    public boolean moveToSprint(String issueId, String sprintId) {
        try {
            if (issueId == null || sprintId == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("planItemId", issueId);
            restTemplate.postForObject(
                    planServiceUrl + "/api/plans/sprints/" + sprintId + "/issues",
                    new HttpEntity<>(body, headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("moveToSprint failed for issue {} to sprint {}: {}", issueId, sprintId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public boolean moveToBacklog(String issueId) {
        try {
            if (issueId == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("planItemIds", List.of(issueId));
            restTemplate.postForObject(
                    planServiceUrl + "/api/plans/backlog/issues",
                    new HttpEntity<>(body, headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("moveToBacklog failed for issue {}: {}", issueId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public Map<String, Object> getBoard(String boardId) {
        try {
            if (boardId == null) return Map.of();
            Map<?, ?> response = restTemplate.getForObject(
                    planServiceUrl + "/api/plans/boards/" + boardId, Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("getBoard failed for {}: {}", boardId, e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSprintIssues(String sprintId) {
        try {
            if (sprintId == null) return List.of();
            List<?> response = restTemplate.getForObject(
                    planServiceUrl + "/api/plans/sprints/" + sprintId + "/issues",
                    List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(toStringMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("getSprintIssues failed for sprint {}: {}", sprintId, e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    public Map<String, Object> createSprint(String boardId, String name, String startDate, String endDate) {
        try {
            if (boardId == null || name == null) return Map.of();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            if (startDate != null) body.put("startDate", startDate);
            if (endDate != null) body.put("endDate", endDate);
            Map<?, ?> response = restTemplate.postForObject(
                    planServiceUrl + "/api/plans/boards/" + boardId + "/sprints",
                    new HttpEntity<>(body, headers), Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("createSprint failed for board {}: {}", boardId, e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    public boolean closeSprint(String sprintId) {
        try {
            if (sprintId == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(
                    planServiceUrl + "/api/plans/sprints/" + sprintId + "/close",
                    new HttpEntity<>(Map.of(), headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("closeSprint failed for sprint {}: {}", sprintId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public Map<String, Object> getEpic(String epicId) {
        try {
            if (epicId == null) return Map.of();
            Map<?, ?> response = restTemplate.getForObject(
                    issueServiceUrl + "/api/issues/" + epicId, Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("getEpic failed for {}: {}", epicId, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> toStringMap(Map<?, ?> raw) {
        Map<String, Object> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
}
