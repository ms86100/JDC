package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
public class JdcLdapApi {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public JdcLdapApi(RestTemplate restTemplate, String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(String query) {
        try {
            if (query == null || query.isBlank()) return List.of();
            List<?> response = restTemplate.getForObject(
                    userServiceUrl + "/rest/admin/1.0/users/search?search=" + query + "&size=50",
                    List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> user = new HashMap<>();
                    m.forEach((k, v) -> user.put(String.valueOf(k), v));
                    result.add(user);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("LDAP/user search failed: {}", e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUser(String userId) {
        try {
            if (userId == null) return Map.of();
            Map<?, ?> response = restTemplate.getForObject(
                    userServiceUrl + "/rest/admin/1.0/users/" + userId, Map.class);
            if (response == null) return Map.of();
            Map<String, Object> result = new HashMap<>();
            response.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        } catch (Exception e) {
            log.warn("LDAP/user lookup failed: {}", e.getMessage());
            return Map.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getGroupMembers(String groupName) {
        try {
            if (groupName == null) return List.of();
            Map<?, ?> group = restTemplate.getForObject(
                    userServiceUrl + "/rest/admin/1.0/groups/name/" + groupName, Map.class);
            if (group == null || group.get("id") == null) return List.of();
            List<?> response = restTemplate.getForObject(
                    userServiceUrl + "/rest/admin/1.0/groups/" + group.get("id") + "/members",
                    List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> member = new HashMap<>();
                    m.forEach((k, v) -> member.put(String.valueOf(k), v));
                    result.add(member);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Group member lookup failed: {}", e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getGroups(String query) {
        try {
            List<?> response = restTemplate.getForObject(
                    userServiceUrl + "/rest/admin/1.0/groups" +
                            (query != null ? "?search=" + query : ""),
                    List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> g = new HashMap<>();
                    m.forEach((k, v) -> g.put(String.valueOf(k), v));
                    result.add(g);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Group search failed: {}", e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    public boolean authenticate(String username, String password) {
        try {
            if (username == null || password == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = Map.of("username", username, "password", password);
            Map<?, ?> response = restTemplate.postForObject(
                    userServiceUrl + "/api/admin/auth/verify",
                    new HttpEntity<>(body, headers), Map.class);
            return response != null && Boolean.TRUE.equals(response.get("authenticated"));
        } catch (Exception e) {
            log.warn("LDAP authenticate failed: {}", e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public boolean modify(String userId, Map<String, Object> attributes) {
        try {
            if (userId == null || attributes == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.put(userServiceUrl + "/api/admin/users/" + userId,
                    new HttpEntity<>(attributes, headers));
            return true;
        } catch (Exception e) {
            log.warn("LDAP modify failed for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
