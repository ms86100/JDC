package com.jira.version.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReleaseHubService {

    @Value("${issue.service.url:http://jira-issue-service:8084}")
    private String issueServiceUrl;

    private final RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getReleaseStatus(UUID versionId) {
        List<Map<String, Object>> issues = fetchIssuesForVersion(versionId);

        int todo = 0, inProgress = 0, done = 0;
        int totalPoints = 0, completedPoints = 0;

        for (Map<String, Object> issue : issues) {
            String statusCategory = getStatusCategory(issue);
            int points = issue.get("storyPoints") != null ? ((Number) issue.get("storyPoints")).intValue() : 0;
            totalPoints += points;

            switch (statusCategory) {
                case "DONE" -> { done++; completedPoints += points; }
                case "IN_PROGRESS" -> inProgress++;
                default -> todo++;
            }
        }

        int total = issues.size();
        double progressPercent = total > 0 ? Math.round((double) done / total * 10000.0) / 100.0 : 0;

        return Map.of(
                "versionId", versionId,
                "totalIssues", total,
                "todo", todo,
                "inProgress", inProgress,
                "done", done,
                "totalPoints", totalPoints,
                "completedPoints", completedPoints,
                "progressPercent", progressPercent,
                "issuesByStatus", Map.of("TODO", todo, "IN_PROGRESS", inProgress, "DONE", done)
        );
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getReleaseWarnings(UUID versionId) {
        List<Map<String, Object>> issues = fetchIssuesForVersion(versionId);
        List<Map<String, Object>> warnings = new ArrayList<>();

        for (Map<String, Object> issue : issues) {
            String statusCategory = getStatusCategory(issue);
            String issueKey = issue.getOrDefault("issueKey", "").toString();

            if (!"DONE".equals(statusCategory)) {
                Object devInfo = issue.get("devInfo");
                if (devInfo == null) {
                    try {
                        Map<String, Object> di = restTemplate.getForObject(
                                issueServiceUrl + "/api/issues/" + issue.get("id") + "/dev-info", Map.class);
                        if (di != null) {
                            long commitCount = di.get("commitCount") != null ? ((Number) di.get("commitCount")).longValue() : 0;
                            long prCount = di.get("pullRequestCount") != null ? ((Number) di.get("pullRequestCount")).longValue() : 0;
                            List openPrs = di.get("openPullRequests") != null ? (List) di.get("openPullRequests") : List.of();

                            if (commitCount == 0) {
                                warnings.add(Map.of("issueKey", issueKey, "type", "NO_COMMITS",
                                        "message", "No commits linked to this issue"));
                            }
                            if (!openPrs.isEmpty()) {
                                warnings.add(Map.of("issueKey", issueKey, "type", "OPEN_PULL_REQUEST",
                                        "message", openPrs.size() + " open pull request(s) need to be merged"));
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Could not fetch dev info for {}: {}", issueKey, e.getMessage());
                    }
                }
            }
        }

        return Map.of("versionId", versionId, "warnings", warnings, "warningCount", warnings.size());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchIssuesForVersion(UUID versionId) {
        try {
            String url = issueServiceUrl + "/api/issues?fixVersion=" + versionId + "&size=1000";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("content") != null) {
                return (List<Map<String, Object>>) response.get("content");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch issues for version {}: {}", versionId, e.getMessage());
        }
        return List.of();
    }

    private String getStatusCategory(Map<String, Object> issue) {
        String cat = issue.get("statusCategory") != null ? issue.get("statusCategory").toString() : "";
        if (cat.isEmpty()) {
            String status = issue.get("statusName") != null ? issue.get("statusName").toString() : "";
            if (status.contains("Done") || status.contains("Closed") || status.contains("Resolved")) return "DONE";
            if (status.contains("Progress") || status.contains("Review")) return "IN_PROGRESS";
            return "TODO";
        }
        return cat.toUpperCase();
    }
}
