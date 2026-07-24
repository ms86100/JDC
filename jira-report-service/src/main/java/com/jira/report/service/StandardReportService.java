package com.jira.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StandardReportService {

    @Value("${issue.service.url:http://jira-issue-service:8084}")
    private String issueServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCreatedVsResolved(UUID projectId, String period, String startDate, String endDate) {
        String jql = String.format("project = \"%s\"", projectId);
        List<Map<String, Object>> issues = fetchIssues(jql);

        Map<String, Integer> created = new TreeMap<>();
        Map<String, Integer> resolved = new TreeMap<>();

        for (Map<String, Object> issue : issues) {
            String createdPeriod = extractPeriod(issue.get("createdAt"), period);
            if (createdPeriod != null) created.merge(createdPeriod, 1, Integer::sum);

            if (issue.get("resolutionDate") != null) {
                String resolvedPeriod = extractPeriod(issue.get("resolutionDate"), period);
                if (resolvedPeriod != null) resolved.merge(resolvedPeriod, 1, Integer::sum);
            }
        }

        return Map.of("projectId", projectId, "period", period, "created", created, "resolved", resolved, "totalIssues", issues.size());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAverageAge(UUID projectId, String groupBy) {
        List<Map<String, Object>> issues = fetchIssues(String.format("project = \"%s\" AND resolution is EMPTY", projectId));

        Map<String, List<Long>> groups = new LinkedHashMap<>();
        long now = System.currentTimeMillis();

        for (Map<String, Object> issue : issues) {
            String key = resolveGroupKey(issue, groupBy);
            long createdMs = parseTimestampMs(issue.get("createdAt"));
            long ageDays = createdMs > 0 ? (now - createdMs) / (1000 * 60 * 60 * 24) : 0;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(ageDays);
        }

        Map<String, Double> averages = new LinkedHashMap<>();
        groups.forEach((k, v) -> averages.put(k, v.stream().mapToLong(Long::longValue).average().orElse(0)));

        return Map.of("projectId", projectId, "groupBy", groupBy != null ? groupBy : "all", "averageAgeDays", averages, "totalUnresolved", issues.size());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResolutionTime(UUID projectId, String period) {
        List<Map<String, Object>> issues = fetchIssues(String.format("project = \"%s\" AND resolution is not EMPTY", projectId));

        Map<String, List<Long>> groups = new TreeMap<>();
        for (Map<String, Object> issue : issues) {
            long created = parseTimestampMs(issue.get("createdAt"));
            long resolved = parseTimestampMs(issue.get("resolutionDate"));
            if (created > 0 && resolved > 0) {
                long daysToResolve = (resolved - created) / (1000 * 60 * 60 * 24);
                String periodKey = extractPeriod(issue.get("resolutionDate"), period);
                if (periodKey != null) groups.computeIfAbsent(periodKey, k -> new ArrayList<>()).add(daysToResolve);
            }
        }

        Map<String, Double> averages = new LinkedHashMap<>();
        groups.forEach((k, v) -> averages.put(k, v.stream().mapToLong(Long::longValue).average().orElse(0)));

        return Map.of("projectId", projectId, "period", period != null ? period : "MONTHLY", "averageResolutionDays", averages);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getGroupBy(UUID projectId, String field) {
        List<Map<String, Object>> issues = fetchIssues(String.format("project = \"%s\"", projectId));

        Map<String, Integer> groups = new LinkedHashMap<>();
        for (Map<String, Object> issue : issues) {
            String key = resolveGroupKey(issue, field);
            groups.merge(key, 1, Integer::sum);
        }

        return Map.of("projectId", projectId, "field", field, "groups", groups, "totalIssues", issues.size());
    }

    public Map<String, Object> getPieChart(UUID projectId, String field) {
        return getGroupBy(projectId, field);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRecentlyCreated(UUID projectId, int days, String period) {
        List<Map<String, Object>> issues = fetchIssues(String.format("project = \"%s\" AND created >= -%dd", projectId, days));

        Map<String, Integer> created = new TreeMap<>();
        for (Map<String, Object> issue : issues) {
            String periodKey = extractPeriod(issue.get("createdAt"), period);
            if (periodKey != null) created.merge(periodKey, 1, Integer::sum);
        }

        return Map.of("projectId", projectId, "days", days, "period", period != null ? period : "DAILY", "created", created, "totalIssues", issues.size());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTimeSince(UUID projectId, String field, String olderThan) {
        List<Map<String, Object>> issues = fetchIssues(String.format("project = \"%s\"", projectId));

        int thresholdDays = parseThresholdDays(olderThan);
        long now = System.currentTimeMillis();
        List<Map<String, String>> staleIssues = new ArrayList<>();

        for (Map<String, Object> issue : issues) {
            long fieldMs = parseTimestampMs(issue.get(field != null ? field : "updatedAt"));
            if (fieldMs > 0) {
                long daysSince = (now - fieldMs) / (1000 * 60 * 60 * 24);
                if (daysSince >= thresholdDays) {
                    staleIssues.add(Map.of(
                            "issueKey", String.valueOf(issue.getOrDefault("issueKey", "")),
                            "daysSince", String.valueOf(daysSince)
                    ));
                }
            }
        }

        return Map.of("projectId", projectId, "field", field != null ? field : "updatedAt", "thresholdDays", thresholdDays, "issues", staleIssues, "totalStale", staleIssues.size());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getVersionWorkload(UUID versionId) {
        List<Map<String, Object>> issues = fetchIssues(String.format("fixVersion = \"%s\" AND resolution is EMPTY", versionId));

        Map<String, Map<String, Object>> assigneeWorkload = new LinkedHashMap<>();
        for (Map<String, Object> issue : issues) {
            String assignee = issue.get("assigneeName") != null ? issue.get("assigneeName").toString() : "Unassigned";
            assigneeWorkload.computeIfAbsent(assignee, k -> new LinkedHashMap<>(Map.of("issueCount", 0, "storyPoints", 0)));
            Map<String, Object> workload = assigneeWorkload.get(assignee);
            workload.put("issueCount", (int) workload.get("issueCount") + 1);
            int points = issue.get("storyPoints") != null ? ((Number) issue.get("storyPoints")).intValue() : 0;
            workload.put("storyPoints", (int) workload.get("storyPoints") + points);
        }

        return Map.of("versionId", versionId, "assigneeWorkload", assigneeWorkload, "totalUnresolved", issues.size());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserWorkload(UUID userId) {
        List<Map<String, Object>> issues = fetchIssues(String.format("assignee = \"%s\" AND resolution is EMPTY", userId));

        Map<String, Map<String, Object>> projectWorkload = new LinkedHashMap<>();
        for (Map<String, Object> issue : issues) {
            String project = issue.get("projectKey") != null ? issue.get("projectKey").toString() : "Unknown";
            projectWorkload.computeIfAbsent(project, k -> new LinkedHashMap<>(Map.of("issueCount", 0, "storyPoints", 0)));
            Map<String, Object> workload = projectWorkload.get(project);
            workload.put("issueCount", (int) workload.get("issueCount") + 1);
            int points = issue.get("storyPoints") != null ? ((Number) issue.get("storyPoints")).intValue() : 0;
            workload.put("storyPoints", (int) workload.get("storyPoints") + points);
        }

        return Map.of("userId", userId, "projectWorkload", projectWorkload, "totalUnresolved", issues.size());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchIssues(String jql) {
        try {
            String url = issueServiceUrl + "/api/issues?jql=" + java.net.URLEncoder.encode(jql, "UTF-8") + "&size=1000";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("content") != null) {
                return (List<Map<String, Object>>) response.get("content");
            }
            if (response != null && response instanceof List) {
                return (List<Map<String, Object>>) response;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch issues for report: {}", e.getMessage());
        }
        return List.of();
    }

    private String extractPeriod(Object timestamp, String period) {
        if (timestamp == null) return null;
        String ts = timestamp.toString();
        if (ts.length() < 10) return null;
        return switch (period != null ? period.toUpperCase() : "MONTHLY") {
            case "DAILY" -> ts.substring(0, 10);
            case "WEEKLY" -> ts.substring(0, 7) + "-W" + (Integer.parseInt(ts.substring(8, 10)) / 7 + 1);
            case "MONTHLY" -> ts.substring(0, 7);
            default -> ts.substring(0, 7);
        };
    }

    private String resolveGroupKey(Map<String, Object> issue, String field) {
        if (field == null) return "all";
        return switch (field.toLowerCase()) {
            case "status" -> issue.get("statusName") != null ? issue.get("statusName").toString() : "Unknown";
            case "priority" -> issue.get("priorityName") != null ? issue.get("priorityName").toString() : "Unknown";
            case "assignee" -> issue.get("assigneeName") != null ? issue.get("assigneeName").toString() : "Unassigned";
            case "type", "issuetype" -> issue.get("issueTypeName") != null ? issue.get("issueTypeName").toString() : "Unknown";
            case "reporter" -> issue.get("reporterName") != null ? issue.get("reporterName").toString() : "Unknown";
            default -> issue.get(field) != null ? issue.get(field).toString() : "Unknown";
        };
    }

    private long parseTimestampMs(Object ts) {
        if (ts == null) return 0;
        try {
            if (ts instanceof Number) return ((Number) ts).longValue();
            String s = ts.toString();
            if (s.contains("T")) {
                return java.time.LocalDateTime.parse(s.replace("Z", "")).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
            }
            return java.time.LocalDate.parse(s.substring(0, 10)).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseThresholdDays(String olderThan) {
        if (olderThan == null) return 30;
        try {
            String num = olderThan.replaceAll("[^0-9]", "");
            int val = Integer.parseInt(num);
            if (olderThan.contains("w")) return val * 7;
            if (olderThan.contains("m") && !olderThan.contains("min")) return val * 30;
            return val;
        } catch (Exception e) {
            return 30;
        }
    }
}
