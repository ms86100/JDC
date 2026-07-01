package com.jira.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jira.search.dto.IndexRequest;
import com.jira.search.dto.ReindexStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReindexService {

    private final SearchService searchService;
    private final RestTemplate restTemplate;

    @Value("${services.issue.url:http://localhost:8084}")
    private String issueServiceUrl;

    @Value("${services.project.url:http://localhost:8083}")
    private String projectServiceUrl;

    public ReindexStatusResponse reindexEntityType(JsonNode body) {
        String entityType = parseEntityType(body);
        LocalDateTime start = LocalDateTime.now();
        long indexed = 0;
        long failed = 0;
        long total = 0;
        String errorMessage = null;

        try {
            switch (entityType) {
                case "ISSUE" -> {
                    ReindexCounts counts = reindexIssues();
                    indexed = counts.indexed();
                    failed = counts.failed();
                    total = counts.total();
                }
                case "PROJECT" -> {
                    ReindexCounts counts = reindexProjects();
                    indexed = counts.indexed();
                    failed = counts.failed();
                    total = counts.total();
                }
                case "COMMENT" -> {
                    // Comments are indexed on write; acknowledge post-import reindex.
                    total = 0;
                    indexed = 0;
                    log.info("COMMENT reindex: no bulk source API — skipped (index-on-write)");
                }
                default -> errorMessage = "Unsupported entity type: " + entityType;
            }
        } catch (Exception e) {
            log.warn("Reindex {} failed: {}", entityType, e.getMessage());
            errorMessage = e.getMessage();
        }

        boolean success = errorMessage == null;
        double progress = total > 0 ? (indexed * 100.0) / total : (success ? 100.0 : 0.0);

        return ReindexStatusResponse.builder()
                .entityType(entityType)
                .status(success ? "COMPLETED" : "FAILED")
                .totalDocuments(total)
                .indexedDocuments(indexed)
                .failedDocuments(failed)
                .progressPercentage(progress)
                .startTime(start)
                .endTime(LocalDateTime.now())
                .currentPhase("DONE")
                .errorMessage(errorMessage)
                .success(success)
                .build();
    }

    public ReindexStatusResponse reindexAll() {
        long indexed = 0;
        long failed = 0;
        long total = 0;
        for (String type : List.of("ISSUE", "PROJECT", "COMMENT")) {
            ReindexStatusResponse row = reindexEntityType(
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.textNode(type));
            indexed += row.getIndexedDocuments();
            failed += row.getFailedDocuments();
            total += row.getTotalDocuments();
        }
        return ReindexStatusResponse.builder()
                .entityType("ALL")
                .status("COMPLETED")
                .totalDocuments(total)
                .indexedDocuments(indexed)
                .failedDocuments(failed)
                .progressPercentage(total > 0 ? (indexed * 100.0) / total : 100.0)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .success(true)
                .build();
    }

    private String parseEntityType(JsonNode body) {
        if (body == null || body.isNull()) {
            return "ISSUE";
        }
        if (body.isTextual()) {
            return body.asText().trim().toUpperCase(Locale.ROOT);
        }
        if (body.has("entityType")) {
            return body.get("entityType").asText("ISSUE").trim().toUpperCase(Locale.ROOT);
        }
        return "ISSUE";
    }

    private ReindexCounts reindexIssues() {
        String base = issueServiceUrl.replaceAll("/+$", "");
        int page = 0;
        int size = 100;
        long indexed = 0;
        long failed = 0;
        long total = 0;

        while (true) {
            String url = base + "/api/issues?page=" + page + "&size=" + size;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            Map<String, Object> body = response.getBody();
            if (body == null) {
                break;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = body.get("content") instanceof List<?> list
                    ? (List<Map<String, Object>>) list
                    : List.of();

            if (content.isEmpty()) {
                break;
            }

            total += content.size();
            for (Map<String, Object> issue : content) {
                try {
                    UUID id = UUID.fromString(String.valueOf(issue.get("id")));
                    String title = firstNonBlank(
                            issue.get("title"),
                            issue.get("issueKey"),
                            issue.get("id"));
                    String description = issue.get("description") != null
                            ? String.valueOf(issue.get("description"))
                            : "";

                    searchService.indexEntity(IndexRequest.builder()
                            .entityType("ISSUE")
                            .entityId(id)
                            .title(title)
                            .content(description)
                            .build());
                    indexed++;
                } catch (Exception e) {
                    failed++;
                    log.debug("Failed to index issue {}: {}", issue.get("id"), e.getMessage());
                }
            }

            Object totalPages = body.get("totalPages");
            page++;
            if (totalPages instanceof Number n && page >= n.intValue()) {
                break;
            }
            if (content.size() < size) {
                break;
            }
        }

        return new ReindexCounts(indexed, failed, total);
    }

    private ReindexCounts reindexProjects() {
        String base = projectServiceUrl.replaceAll("/+$", "");
        String url = base + "/api/projects";
        long indexed = 0;
        long failed = 0;

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> projects = response.getBody() != null ? response.getBody() : List.of();
            long total = projects.size();

            for (Map<String, Object> project : projects) {
                try {
                    UUID id = UUID.fromString(String.valueOf(project.get("id")));
                    String name = firstNonBlank(project.get("name"), project.get("projectKey"), project.get("id"));
                    String desc = project.get("description") != null
                            ? String.valueOf(project.get("description"))
                            : "";

                    searchService.indexEntity(IndexRequest.builder()
                            .entityType("PROJECT")
                            .entityId(id)
                            .title(name)
                            .content(desc)
                            .build());
                    indexed++;
                } catch (Exception e) {
                    failed++;
                }
            }
            return new ReindexCounts(indexed, failed, total);
        } catch (Exception e) {
            log.warn("Project reindex skipped (project-service unavailable): {}", e.getMessage());
            return new ReindexCounts(0, 0, 0);
        }
    }

    private String firstNonBlank(Object... values) {
        for (Object v : values) {
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return "unknown";
    }

    private record ReindexCounts(long indexed, long failed, long total) {}
}
