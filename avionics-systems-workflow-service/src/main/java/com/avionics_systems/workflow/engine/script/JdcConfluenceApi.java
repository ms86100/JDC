package com.avionics_systems.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JdcConfluenceApi {

    private final RestTemplate restTemplate;
    private final String confluenceUrl;

    public JdcConfluenceApi(RestTemplate restTemplate, String confluenceUrl) {
        this.restTemplate = restTemplate;
        this.confluenceUrl = confluenceUrl;
    }

    @HostAccess.Export
    public Map<String, Object> getPage(String pageId) {
        try {
            if (pageId == null || confluenceUrl == null || confluenceUrl.isBlank()) return Map.of();
            Map<?, ?> response = restTemplate.getForObject(
                    confluenceUrl + "/rest/api/content/" + pageId + "?expand=body.storage,version",
                    Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("Confluence getPage failed: {}", e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    public Map<String, Object> createPage(String spaceKey, String title, String htmlBody, String parentId) {
        try {
            if (confluenceUrl == null || confluenceUrl.isBlank()) return Map.of("error", "Confluence URL not configured");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("type", "page");
            body.put("title", title);
            body.put("space", Map.of("key", spaceKey));
            body.put("body", Map.of("storage", Map.of("value", htmlBody, "representation", "storage")));
            if (parentId != null) {
                body.put("ancestors", java.util.List.of(Map.of("id", parentId)));
            }
            Map<?, ?> response = restTemplate.postForObject(
                    confluenceUrl + "/rest/api/content",
                    new HttpEntity<>(body, headers), Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("Confluence createPage failed: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    @HostAccess.Export
    public Map<String, Object> updatePage(String pageId, String title, String htmlBody, int version) {
        try {
            if (confluenceUrl == null || confluenceUrl.isBlank()) return Map.of("error", "Confluence URL not configured");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("type", "page");
            body.put("title", title);
            body.put("body", Map.of("storage", Map.of("value", htmlBody, "representation", "storage")));
            body.put("version", Map.of("number", version));
            restTemplate.put(confluenceUrl + "/rest/api/content/" + pageId,
                    new HttpEntity<>(body, headers));
            return Map.of("success", true, "pageId", pageId);
        } catch (Exception e) {
            log.warn("Confluence updatePage failed: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> search(String cql, int limit) {
        try {
            if (confluenceUrl == null || confluenceUrl.isBlank()) return java.util.List.of();
            Map<?, ?> response = restTemplate.getForObject(
                    confluenceUrl + "/rest/api/content/search?cql=" + cql + "&limit=" + Math.min(limit, 100),
                    Map.class);
            if (response != null && response.get("results") instanceof java.util.List<?> results) {
                java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
                for (Object r : results) {
                    if (r instanceof Map<?, ?> m) list.add(toStringMap(m));
                }
                return list;
            }
            return java.util.List.of();
        } catch (Exception e) {
            log.warn("Confluence search failed: {}", e.getMessage());
            return java.util.List.of();
        }
    }

    @HostAccess.Export
    public boolean deletePage(String pageId) {
        try {
            if (pageId == null || confluenceUrl == null || confluenceUrl.isBlank()) return false;
            restTemplate.delete(confluenceUrl + "/rest/api/content/" + pageId);
            return true;
        } catch (Exception e) {
            log.warn("Confluence deletePage failed: {}", e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> getPageChildren(String pageId) {
        try {
            if (pageId == null || confluenceUrl == null || confluenceUrl.isBlank()) return java.util.List.of();
            Map<?, ?> response = restTemplate.getForObject(
                    confluenceUrl + "/rest/api/content/" + pageId + "/child/page", Map.class);
            if (response != null && response.get("results") instanceof java.util.List<?> results) {
                java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
                for (Object r : results) {
                    if (r instanceof Map<?, ?> m) list.add(toStringMap(m));
                }
                return list;
            }
            return java.util.List.of();
        } catch (Exception e) {
            log.warn("Confluence getPageChildren failed: {}", e.getMessage());
            return java.util.List.of();
        }
    }

    @HostAccess.Export
    public boolean addPageLabel(String pageId, String labelName) {
        try {
            if (pageId == null || labelName == null || confluenceUrl == null || confluenceUrl.isBlank()) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            java.util.List<Map<String, String>> labels = java.util.List.of(Map.of("prefix", "global", "name", labelName));
            restTemplate.postForObject(confluenceUrl + "/rest/api/content/" + pageId + "/label",
                    new HttpEntity<>(labels, headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Confluence addPageLabel failed: {}", e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public Map<String, Object> getSpace(String spaceKey) {
        try {
            if (spaceKey == null || confluenceUrl == null || confluenceUrl.isBlank()) return Map.of();
            Map<?, ?> response = restTemplate.getForObject(
                    confluenceUrl + "/rest/api/space/" + spaceKey, Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("Confluence getSpace failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> toStringMap(Map<?, ?> raw) {
        Map<String, Object> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
}
