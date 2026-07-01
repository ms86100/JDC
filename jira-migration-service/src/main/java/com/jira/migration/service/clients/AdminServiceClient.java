package com.jira.migration.service.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxies Jira admin-service issue configuration APIs for migration import.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.adminServiceUrl:http://localhost:8093}")
    private String adminServiceUrl;

    private static final String ISSUES_ADMIN = "/api/admin/issues";

    public Map<String, Object> createScreen(Map<String, Object> data) {
        return post(ISSUES_ADMIN + "/screens", data);
    }

    public Map<String, Object> createScreenScheme(Map<String, Object> data) {
        return post(ISSUES_ADMIN + "/screen-schemes", data);
    }

    public Map<String, Object> createPermissionScheme(Map<String, Object> data) {
        return post(ISSUES_ADMIN + "/permission-schemes", data);
    }

    public Map<String, Object> createNotificationScheme(Map<String, Object> data) {
        return post(ISSUES_ADMIN + "/notification-schemes", data);
    }

    public List<Map<String, Object>> listScreens() {
        try {
            List<Map<String, Object>> body = restTemplate.exchange(
                    base() + ISSUES_ADMIN + "/screens",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return body != null ? body : List.of();
        } catch (Exception e) {
            log.warn("listScreens failed: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean isAvailable() {
        try {
            restTemplate.getForEntity(base() + "/actuator/health", Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Object response = restTemplate.postForObject(base() + path, entity, Object.class);
        if (response instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("raw", response);
        return out;
    }

    private String base() {
        return adminServiceUrl.replaceAll("/$", "");
    }
}
