package com.jira.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class ProjectNotificationSchemeClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jira.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @SuppressWarnings("unchecked")
    public Set<UUID> resolveRecipients(UUID projectId, String eventType) {
        if (projectId == null) {
            return Set.of();
        }
        try {
            Map<?, ?> scheme = restTemplate.getForObject(
                    projectServiceUrl + "/api/projects/" + projectId + "/scheme", Map.class);
            if (scheme == null) {
                return Set.of();
            }
            Object notif = scheme.get("notificationScheme");
            if (!(notif instanceof Map<?, ?> notifMap)) {
                return Set.of();
            }
            Object raw = notifMap.get("notifications");
            if (raw == null) {
                return Set.of();
            }
            JsonNode root = objectMapper.readTree(raw.toString());
            if (!root.isArray()) {
                return Set.of();
            }
            Set<UUID> recipients = new LinkedHashSet<>();
            for (JsonNode entry : root) {
                String evt = text(entry, "eventType", "event", "type");
                if (evt != null && !matchesEvent(evt, eventType)) {
                    continue;
                }
                addRecipients(recipients, entry.get("recipients"));
                addRecipients(recipients, entry.get("userIds"));
                String single = text(entry, "userId", "recipientId");
                if (single != null) {
                    parseUuid(single).ifPresent(recipients::add);
                }
            }
            return recipients;
        } catch (Exception e) {
            log.debug("Notification scheme lookup skipped for project {}: {}", projectId, e.getMessage());
            return Set.of();
        }
    }

    private static boolean matchesEvent(String configured, String requested) {
        String a = configured.trim().toUpperCase(Locale.ROOT);
        String b = requested.trim().toUpperCase(Locale.ROOT);
        return a.equals(b)
                || a.equals("ISSUE_TRANSITIONED") && b.contains("TRANSITION")
                || a.equals("ISSUE_UPDATED") && b.contains("TRANSITION");
    }

    private void addRecipients(Set<UUID> out, JsonNode node) {
        if (node == null || !node.isArray()) {
            return;
        }
        for (JsonNode n : node) {
            parseUuid(n.asText()).ifPresent(out::add);
        }
    }

    private static Optional<UUID> parseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String text(JsonNode node, String... fields) {
        for (String f : fields) {
            if (node.has(f) && !node.get(f).isNull()) {
                return node.get(f).asText();
            }
        }
        return null;
    }
}
