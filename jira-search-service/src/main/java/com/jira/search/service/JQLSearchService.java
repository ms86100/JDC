package com.jira.search.service;

import com.jira.search.dto.JQLSearchRequest;
import com.jira.search.dto.JQLSearchResponse;
import com.jira.search.dto.JQLSearchResponse.IssueSummary;
import com.jira.search.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JQL Search Service - Full JQL implementation matching Jira DC
 * Now with real search execution via jira-issue-service integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JQLSearchService {

    private final JQLParser jqlParser;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ISSUE_SERVICE_URL = "http://jira-issue-service:8084";

    /**
     * Parse and validate JQL query
     */
    public JQLQuery parseQuery(String jql) {
        log.info("Parsing JQL: {}", jql);
        JQLQuery query = jqlParser.parse(jql);
        validateQuery(query);
        return query;
    }

    /**
     * Search using JQL - Now with real execution
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public JQLSearchResponse search(JQLSearchRequest request) {
        log.info("Executing JQL search: {}", request.getJql());

        try {
            // Parse JQL query
            JQLQuery query = jqlParser.parse(request.getJql());

            // Extract the SQL WHERE clause for issue filtering
            String sqlWhereClause = jqlParser.toSqlWhereClause(query);
            log.debug("Generated SQL WHERE clause: {}", sqlWhereClause);

            // Call issue service to execute search
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("jql", request.getJql());
            searchParams.put("page", request.getPage());
            searchParams.put("pageSize", request.getPageSize());

            // Build the search URL with query parameters
            String searchUrl = ISSUE_SERVICE_URL + "/api/issues/search?jql=" +
                    java.net.URLEncoder.encode(request.getJql(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&page=" + request.getPage() +
                    "&pageSize=" + request.getPageSize();

            log.debug("Calling issue service: {}", searchUrl);

            // Execute search via REST
            Map<String, Object> response = restTemplate.getForObject(searchUrl, Map.class);

            if (response != null && response.containsKey("issues")) {
                List<Map<String, Object>> issuesList = (List<Map<String, Object>>) response.get("issues");

                List<IssueSummary> issues = issuesList.stream()
                        .map(this::mapToIssueSummary)
                        .collect(Collectors.toList());

                long totalCount = response.containsKey("totalCount") ?
                        ((Number) response.get("totalCount")).longValue() : issues.size();

                return JQLSearchResponse.builder()
                        .issues(issues)
                        .totalCount(totalCount)
                        .page(request.getPage())
                        .pageSize(request.getPageSize())
                        .parsedQuery(query)
                        .build();
            }

            // Fallback if no issues found
            return JQLSearchResponse.builder()
                    .issues(new ArrayList<>())
                    .totalCount(0)
                    .page(request.getPage())
                    .pageSize(request.getPageSize())
                    .message("No issues found matching the query")
                    .parsedQuery(query)
                    .build();

        } catch (Exception e) {
            log.error("JQL search failed: {}", e.getMessage(), e);

            // Return error response
            return JQLSearchResponse.builder()
                    .issues(new ArrayList<>())
                    .totalCount(0)
                    .page(request.getPage())
                    .pageSize(request.getPageSize())
                    .message("Search failed: " + e.getMessage())
                    .build();
        }
    }

    private IssueSummary mapToIssueSummary(Map<String, Object> issue) {
        Map<String, Object> fields = (Map<String, Object>) issue.getOrDefault("fields", new HashMap<>());

        return IssueSummary.builder()
                .issueKey((String) issue.getOrDefault("key", issue.getOrDefault("issueKey", "")))
                .summary((String) fields.getOrDefault("summary", ""))
                .status(extractStatusName(fields))
                .issueType(extractIssueTypeName(fields))
                .assignee(extractUserName(fields.get("assignee")))
                .reporter(extractUserName(fields.get("reporter")))
                .priority(extractPriorityName(fields))
                .build();
    }

    private String extractStatusName(Map<String, Object> fields) {
        Object status = fields.get("status");
        if (status instanceof Map) {
            Object name = ((Map<?, ?>) status).get("name");
            return name != null ? name.toString() : "Unknown";
        }
        return status != null ? status.toString() : "Unknown";
    }

    private String extractIssueTypeName(Map<String, Object> fields) {
        Object issueType = fields.get("issuetype");
        if (issueType instanceof Map) {
            Object name = ((Map<?, ?>) issueType).get("name");
            return name != null ? name.toString() : "Task";
        }
        return issueType != null ? issueType.toString() : "Task";
    }

    private String extractPriorityName(Map<String, Object> fields) {
        Object priority = fields.get("priority");
        if (priority instanceof Map) {
            Object name = ((Map<?, ?>) priority).get("name");
            return name != null ? name.toString() : "Medium";
        }
        return priority != null ? priority.toString() : "Medium";
    }

    private String extractUserName(Object user) {
        if (user instanceof Map) {
            Object name = ((Map<?, ?>) user).get("displayName");
            return name != null ? name.toString() : "";
        }
        return user != null ? user.toString() : "";
    }

    /**
     * Get autocomplete suggestions for JQL fields
     */
    public List<String> getFieldSuggestions(String prefix) {
        List<String> fields = List.of(
                "issue", "issuekey", "key", "summary", "description", "environment",
                "type", "issuetype", "status", "priority", "resolution", "assignee",
                "reporter", "creator", "created", "updated", "duedate", "resolved",
                "votes", "watchers", "comment", "attachment", "subtask", "parent",
                "project", "sprint", "epic", "epiclink", "epic name", "labels",
                "component", "fixversion", "affectsversion", "linkedissue",
                "flagged", "resolved", "resolutiondate", "lastviewed",
                "workratio", "security", "securitylevel", "timespent",
                "timeestimate", "timeoriginalestimate", "aggregatetimespent",
                "aggregatetimeestimate", "aggregatetimeoriginalestimate",
                "progress", "originalestimate", "remainingestimate"
        );

        return fields.stream()
                .filter(f -> f.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get autocomplete suggestions for JQL operators
     */
    public List<String> getOperatorSuggestions(String fieldName) {
        List<String> allOperators = List.of(
                "=", "!=", ">", "<", ">=", "<=",
                "~", "!~", "IN", "NOT IN", "IS", "IS NOT",
                "WAS", "WAS NOT", "WAS IN", "WAS NOT IN", "CHANGED"
        );

        Set<String> textFields = Set.of("summary", "description", "comment", "environment", "text");
        if (textFields.contains(fieldName.toLowerCase())) {
            return allOperators;
        }

        Set<String> dateFields = Set.of("created", "updated", "duedate", "resolved", "resolutiondate");
        if (dateFields.contains(fieldName.toLowerCase())) {
            return allOperators;
        }

        return List.of("=", "!=", "IN", "NOT IN", "IS", "IS NOT");
    }

    /**
     * Get value suggestions for a field (fetched from actual services)
     */
    public List<String> getValueSuggestions(String fieldName, String prefix) {
        try {
            return switch (fieldName.toLowerCase()) {
                case "status" -> fetchFromService("/api/statuses", prefix);
                case "issuetype", "type" -> fetchFromService("/api/issuetypes", prefix);
                case "priority" -> fetchFromService("/api/priorities", prefix);
                case "project" -> fetchProjectsFromService(prefix);
                case "resolution" -> getResolutionSuggestions(prefix);
                case "labels" -> getLabelSuggestions(prefix);
                default -> List.of();
            };
        } catch (Exception e) {
            log.warn("Failed to fetch suggestions for {}: {}", fieldName, e.getMessage());
            return getFallbackSuggestions(fieldName, prefix);
        }
    }

    private List<String> fetchFromService(String endpoint, String prefix) {
        try {
            String url = ISSUE_SERVICE_URL + endpoint;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                return data.stream()
                        .map(item -> (String) item.getOrDefault("name", ""))
                        .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("Service call failed, using fallback: {}", e.getMessage());
        }
        return getFallbackSuggestions(endpoint.replace("/api/", ""), prefix);
    }

    private List<String> fetchProjectsFromService(String prefix) {
        try {
            String url = "http://jira-project-service:8083/api/projects";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("projects")) {
                List<Map<String, Object>> projects = (List<Map<String, Object>>) response.get("projects");
                return projects.stream()
                        .map(project -> (String) project.getOrDefault("projectKey", ""))
                        .filter(key -> !key.isEmpty())
                        .filter(key -> key.toLowerCase().startsWith(prefix.toLowerCase()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("Project service call failed, using fallback: {}", e.getMessage());
        }
        return List.of("DEMO", "PROJ", "TEST", "PROD", "DEV");
    }

    private List<String> getFallbackSuggestions(String fieldName, String prefix) {
        return switch (fieldName.toLowerCase()) {
            case "status", "statuses" -> getStatusSuggestions(prefix);
            case "issuetype", "type", "issuetypes" -> getIssueTypeSuggestions(prefix);
            case "priority", "priorities" -> getPrioritySuggestions(prefix);
            case "resolution" -> getResolutionSuggestions(prefix);
            case "label", "labels" -> getLabelSuggestions(prefix);
            default -> List.of();
        };
    }

    private List<String> getStatusSuggestions(String prefix) {
        return List.of("To Do", "In Progress", "In Review", "Done", "Closed", "Open", "Reopened", "Blocked")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getIssueTypeSuggestions(String prefix) {
        return List.of("Bug", "Story", "Task", "Subtask", "Epic", "Feature", "Improvement", "Spike", "Test")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getPrioritySuggestions(String prefix) {
        return List.of("Highest", "High", "Medium", "Low", "Lowest", "Blocker", "Critical", "Major", "Minor", "Trivial")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getResolutionSuggestions(String prefix) {
        return List.of("Fixed", "Won't Fix", "Duplicate", "Incomplete", "Cannot Reproduce", "Done", "Cancelled")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getLabelSuggestions(String prefix) {
        return List.of("bug", "feature", "enhancement", "documentation", "question", "wontfix",
                       "blocked", "ready-for-review", "in-testing", "tech-debt")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Validate JQL query for common errors
     */
    public List<String> validateQuery(JQLQuery query) {
        List<String> errors = new ArrayList<>();

        if (query == null) {
            errors.add("Query is null");
            return errors;
        }

        if (query.getOriginalJql() != null) {
            long openParens = query.getOriginalJql().chars().filter(c -> c == '(').count();
            long closeParens = query.getOriginalJql().chars().filter(c -> c == ')').count();
            if (openParens != closeParens) {
                errors.add("Unbalanced parentheses in query");
            }
        }

        for (JQLClause clause : query.getClauses()) {
            if (clause.getField() == null || clause.getField().isEmpty()) {
                errors.add("Empty field in clause: " + clause.getValue());
            }
        }

        return errors;
    }

    /**
     * Format JQL for display (syntax highlighting info)
     */
    public String formatJQL(String jql) {
        return jql
                .replace(" AND ", " <span class='jql-operator'>AND</span> ")
                .replace(" OR ", " <span class='jql-operator'>OR</span> ")
                .replace(" ORDER BY ", " <span class='jql-keyword'>ORDER BY</span> ");
    }
}