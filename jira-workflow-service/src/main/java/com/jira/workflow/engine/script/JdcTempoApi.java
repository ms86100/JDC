package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
public class JdcTempoApi {

    private final RestTemplate restTemplate;
    private final String issueServiceUrl;

    public JdcTempoApi(RestTemplate restTemplate, String issueServiceUrl) {
        this.restTemplate = restTemplate;
        this.issueServiceUrl = issueServiceUrl;
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getWorklogs(String issueId) {
        try {
            if (issueId == null) return List.of();
            List<?> response = restTemplate.getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/worklogs", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(toStringMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("getWorklogs failed for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    public Map<String, Object> logWork(String issueId, String timeSpent, String comment, String startedAt) {
        try {
            if (issueId == null || timeSpent == null) return Map.of();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("timeSpent", timeSpent);
            if (comment != null) body.put("comment", comment);
            if (startedAt != null) body.put("startedAt", startedAt);
            Map<?, ?> response = restTemplate.postForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/worklogs",
                    new HttpEntity<>(body, headers), Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("logWork failed for issue {}: {}", issueId, e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    @HostAccess.Export
    public boolean deleteWorklog(String worklogId) {
        try {
            if (worklogId == null) return false;
            restTemplate.delete(issueServiceUrl + "/api/worklogs/" + worklogId);
            return true;
        } catch (Exception e) {
            log.warn("deleteWorklog failed for {}: {}", worklogId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public boolean updateWorklog(String worklogId, String timeSpent, String comment) {
        try {
            if (worklogId == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            if (timeSpent != null) body.put("timeSpent", timeSpent);
            if (comment != null) body.put("comment", comment);
            restTemplate.put(issueServiceUrl + "/api/worklogs/" + worklogId,
                    new HttpEntity<>(body, headers));
            return true;
        } catch (Exception e) {
            log.warn("updateWorklog failed for {}: {}", worklogId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTimeTracking(String issueId) {
        try {
            if (issueId == null) return Map.of();
            Map<?, ?> issue = restTemplate.getForObject(
                    issueServiceUrl + "/api/issues/" + issueId, Map.class);
            if (issue == null) return Map.of();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("originalEstimate", issue.get("originalEstimate"));
            result.put("remainingEstimate", issue.get("remainingEstimate"));
            result.put("timeSpent", issue.get("timeSpent"));

            Object origObj = issue.get("originalEstimate");
            Object spentObj = issue.get("timeSpent");
            if (origObj instanceof Number && spentObj instanceof Number) {
                double original = ((Number) origObj).doubleValue();
                double spent = ((Number) spentObj).doubleValue();
                if (original > 0) {
                    result.put("percentComplete", Math.min(100, Math.round(spent / original * 100)));
                } else {
                    result.put("percentComplete", 0);
                }
            } else {
                result.put("percentComplete", 0);
            }
            return result;
        } catch (Exception e) {
            log.warn("getTimeTracking failed for issue {}: {}", issueId, e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserWorklogs(String userId, String startDate, String endDate) {
        try {
            if (userId == null) return List.of();
            StringBuilder url = new StringBuilder(issueServiceUrl)
                    .append("/api/worklogs?userId=").append(userId);
            if (startDate != null) url.append("&startDate=").append(startDate);
            if (endDate != null) url.append("&endDate=").append(endDate);
            List<?> response = restTemplate.getForObject(url.toString(), List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(toStringMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("getUserWorklogs failed for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProjectTimeReport(String projectId) {
        try {
            if (projectId == null) return Map.of();
            List<?> issues = restTemplate.getForObject(
                    issueServiceUrl + "/api/issues?projectId=" + projectId, List.class);
            if (issues == null) return Map.of();

            long totalTimeSpent = 0;
            long totalOriginalEstimate = 0;
            int issueCount = 0;
            int issuesWithWorklogs = 0;

            for (Object item : issues) {
                if (item instanceof Map<?, ?> m) {
                    issueCount++;
                    Object spent = m.get("timeSpent");
                    Object estimate = m.get("originalEstimate");
                    if (spent instanceof Number) {
                        long spentVal = ((Number) spent).longValue();
                        if (spentVal > 0) {
                            totalTimeSpent += spentVal;
                            issuesWithWorklogs++;
                        }
                    }
                    if (estimate instanceof Number) {
                        totalOriginalEstimate += ((Number) estimate).longValue();
                    }
                }
            }

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("projectId", projectId);
            report.put("totalTimeSpent", totalTimeSpent);
            report.put("totalOriginalEstimate", totalOriginalEstimate);
            report.put("issueCount", issueCount);
            report.put("issuesWithWorklogs", issuesWithWorklogs);
            if (totalOriginalEstimate > 0) {
                report.put("percentComplete", Math.min(100, Math.round((double) totalTimeSpent / totalOriginalEstimate * 100)));
            } else {
                report.put("percentComplete", 0);
            }
            return report;
        } catch (Exception e) {
            log.warn("getProjectTimeReport failed for project {}: {}", projectId, e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public int getTotalTimeSpent(String issueId) {
        try {
            if (issueId == null) return 0;
            List<?> worklogs = restTemplate.getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/worklogs", List.class);
            if (worklogs == null) return 0;
            int total = 0;
            for (Object item : worklogs) {
                if (item instanceof Map<?, ?> m) {
                    Object spent = m.get("timeSpentSeconds");
                    if (spent instanceof Number) {
                        total += ((Number) spent).intValue();
                    }
                }
            }
            return total;
        } catch (Exception e) {
            log.warn("getTotalTimeSpent failed for issue {}: {}", issueId, e.getMessage());
            return 0;
        }
    }

    private Map<String, Object> toStringMap(Map<?, ?> raw) {
        Map<String, Object> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
}
