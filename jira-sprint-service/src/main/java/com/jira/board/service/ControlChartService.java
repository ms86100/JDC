package com.jira.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ControlChartService {

    @Value("${issue.service.url:http://jira-issue-service:8084}")
    private String issueServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public Map<String, Object> getControlChart(UUID boardId, UUID projectId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> issues = fetchResolvedIssues(projectId, startDate, endDate);

        List<Map<String, Object>> dataPoints = new ArrayList<>();
        List<Double> cycleTimes = new ArrayList<>();

        for (Map<String, Object> issue : issues) {
            Double cycleTime = calculateCycleTime(issue);
            if (cycleTime != null && cycleTime > 0) {
                cycleTimes.add(cycleTime);
                dataPoints.add(Map.of(
                        "issueKey", issue.getOrDefault("issueKey", ""),
                        "title", issue.getOrDefault("title", ""),
                        "completedDate", issue.getOrDefault("resolutionDate", ""),
                        "cycleTimeDays", Math.round(cycleTime * 100.0) / 100.0
                ));
            }
        }

        double average = cycleTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        List<Map<String, Object>> rollingAverage = calculateRollingAverage(dataPoints, 5);

        return Map.of(
                "boardId", boardId,
                "projectId", projectId,
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "dataPoints", dataPoints,
                "rollingAverage", rollingAverage,
                "averageCycleTimeDays", Math.round(average * 100.0) / 100.0,
                "totalIssues", dataPoints.size()
        );
    }

    private Double calculateCycleTime(Map<String, Object> issue) {
        try {
            Object createdObj = issue.get("createdAt");
            Object resolvedObj = issue.get("resolutionDate");
            if (createdObj == null || resolvedObj == null) return null;

            long createdMs = parseTimestamp(createdObj.toString());
            long resolvedMs = parseTimestamp(resolvedObj.toString());
            if (createdMs <= 0 || resolvedMs <= 0) return null;

            return (double) (resolvedMs - createdMs) / (1000.0 * 60 * 60 * 24);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> calculateRollingAverage(List<Map<String, Object>> dataPoints, int window) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < dataPoints.size(); i++) {
            int start = Math.max(0, i - window + 1);
            double sum = 0;
            int count = 0;
            for (int j = start; j <= i; j++) {
                sum += ((Number) dataPoints.get(j).get("cycleTimeDays")).doubleValue();
                count++;
            }
            result.add(Map.of(
                    "index", i,
                    "issueKey", dataPoints.get(i).get("issueKey"),
                    "rollingAverageDays", Math.round((sum / count) * 100.0) / 100.0
            ));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchResolvedIssues(UUID projectId, LocalDate startDate, LocalDate endDate) {
        try {
            String jql = String.format("project = \"%s\" AND resolved >= \"%s\" AND resolved <= \"%s\"",
                    projectId, startDate, endDate);
            String url = issueServiceUrl + "/api/issues?jql=" + java.net.URLEncoder.encode(jql, "UTF-8") + "&size=1000";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("content") != null) {
                return (List<Map<String, Object>>) response.get("content");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch resolved issues: {}", e.getMessage());
        }
        return List.of();
    }

    private long parseTimestamp(String ts) {
        try {
            if (ts.contains("T")) {
                return java.time.LocalDateTime.parse(ts.replace("Z", ""))
                        .atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
            }
            return java.time.LocalDate.parse(ts.substring(0, 10))
                    .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }
}
