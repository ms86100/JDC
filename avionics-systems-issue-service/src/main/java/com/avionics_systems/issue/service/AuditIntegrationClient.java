package com.avionics_systems.issue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Posts enterprise audit events to avionics-systems-audit-service (Avionics Systems DC audit trail parity).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditIntegrationClient {

    private final RestTemplate restTemplate;

    @Value("${audit.service.url:http://localhost:8089}")
    private String auditServiceUrl;

    @Async
    public void logIssueEvent(UUID userId, UUID issueId, String action, Map<String, Object> changes) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId != null ? userId.toString() : null);
            body.put("serviceName", "avionics-systems-issue-service");
            body.put("entityType", "ISSUE");
            body.put("entityId", issueId);
            body.put("action", action);
            body.put("changes", changes != null ? changes : Map.of());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) {
                headers.set("X-User-Id", userId.toString());
            }

            restTemplate.postForObject(
                    auditServiceUrl + "/api/audit/logs",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Audit log skipped for issue {} action {}: {}", issueId, action, e.getMessage());
        }
    }
}
