package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
public class JdcAssetApi {

    private final RestTemplate restTemplate;
    private final String adminServiceUrl;

    public JdcAssetApi(RestTemplate restTemplate, String adminServiceUrl) {
        this.restTemplate = restTemplate;
        this.adminServiceUrl = adminServiceUrl;
    }

    @HostAccess.Export
    public Map<String, Object> getAsset(String assetId) {
        try {
            if (assetId == null) return Map.of();
            Map<?, ?> response = restTemplate.getForObject(
                    adminServiceUrl + "/api/admin/assets/" + assetId, Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("getAsset failed for {}: {}", assetId, e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchAssets(String query) {
        try {
            if (query == null || query.isBlank()) return List.of();
            List<?> response = restTemplate.getForObject(
                    adminServiceUrl + "/api/admin/assets?search=" + query, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(toStringMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("searchAssets failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAssetsByType(String assetTypeId) {
        try {
            if (assetTypeId == null) return List.of();
            List<?> response = restTemplate.getForObject(
                    adminServiceUrl + "/api/admin/assets?typeId=" + assetTypeId, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(toStringMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("getAssetsByType failed for type {}: {}", assetTypeId, e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    public Map<String, Object> createAsset(String assetTypeId, String name, Map<String, Object> attributes) {
        try {
            if (assetTypeId == null || name == null) return Map.of();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("assetTypeId", assetTypeId);
            body.put("name", name);
            if (attributes != null) body.put("attributes", attributes);
            Map<?, ?> response = restTemplate.postForObject(
                    adminServiceUrl + "/api/admin/assets",
                    new HttpEntity<>(body, headers), Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("createAsset failed: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    @HostAccess.Export
    public boolean updateAsset(String assetId, Map<String, Object> updates) {
        try {
            if (assetId == null || updates == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.put(adminServiceUrl + "/api/admin/assets/" + assetId,
                    new HttpEntity<>(updates, headers));
            return true;
        } catch (Exception e) {
            log.warn("updateAsset failed for {}: {}", assetId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public boolean deleteAsset(String assetId) {
        try {
            if (assetId == null) return false;
            restTemplate.delete(adminServiceUrl + "/api/admin/assets/" + assetId);
            return true;
        } catch (Exception e) {
            log.warn("deleteAsset failed for {}: {}", assetId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public boolean linkAssetToIssue(String assetId, String issueId, String linkType) {
        try {
            if (assetId == null || issueId == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("issueId", issueId);
            if (linkType != null) body.put("linkType", linkType);
            restTemplate.postForObject(
                    adminServiceUrl + "/api/admin/assets/" + assetId + "/links",
                    new HttpEntity<>(body, headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("linkAssetToIssue failed for asset {} to issue {}: {}", assetId, issueId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public boolean unlinkAssetFromIssue(String assetId, String issueId) {
        try {
            if (assetId == null || issueId == null) return false;
            restTemplate.delete(
                    adminServiceUrl + "/api/admin/assets/" + assetId + "/links/" + issueId);
            return true;
        } catch (Exception e) {
            log.warn("unlinkAssetFromIssue failed for asset {} from issue {}: {}", assetId, issueId, e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAssetLinksForIssue(String issueId) {
        try {
            if (issueId == null) return List.of();
            List<?> response = restTemplate.getForObject(
                    adminServiceUrl + "/api/admin/assets/links/issue/" + issueId, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(toStringMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("getAssetLinksForIssue failed for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAssetTypes() {
        try {
            List<?> response = restTemplate.getForObject(
                    adminServiceUrl + "/api/admin/assets/types", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(toStringMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("getAssetTypes failed: {}", e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    public Map<String, Object> getAssetType(String assetTypeId) {
        try {
            if (assetTypeId == null) return Map.of();
            Map<?, ?> response = restTemplate.getForObject(
                    adminServiceUrl + "/api/admin/assets/types/" + assetTypeId, Map.class);
            return response != null ? toStringMap(response) : Map.of();
        } catch (Exception e) {
            log.warn("getAssetType failed for {}: {}", assetTypeId, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> toStringMap(Map<?, ?> raw) {
        Map<String, Object> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
}
