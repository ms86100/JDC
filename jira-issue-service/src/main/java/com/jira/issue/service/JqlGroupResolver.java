package com.jira.issue.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves Jira {@code membersOf("group")} to user UUIDs via user-service.
 */
@Component
@Slf4j
public class JqlGroupResolver {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${user.service.url:http://localhost:8082}")
    private String userServiceUrl;

    public List<UUID> resolveMembersOf(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return List.of();
        }
        try {
            var groupRes = restTemplate.getForEntity(
                    userServiceUrl + "/rest/admin/1.0/groups/name/" + groupName.trim(),
                    Map.class);
            if (groupRes.getBody() == null || groupRes.getBody().get("id") == null) {
                return List.of();
            }
            UUID groupId = UUID.fromString(groupRes.getBody().get("id").toString());
            ResponseEntity<List<Map<String, Object>>> membersRes = restTemplate.exchange(
                    userServiceUrl + "/rest/admin/1.0/groups/" + groupId + "/members",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});
            if (membersRes.getBody() == null) {
                return List.of();
            }
            List<UUID> ids = new ArrayList<>();
            for (Map<String, Object> user : membersRes.getBody()) {
                Object id = user.get("id");
                if (id != null) {
                    ids.add(UUID.fromString(id.toString()));
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("membersOf({}) resolution failed: {}", groupName, e.getMessage());
            return List.of();
        }
    }
}
