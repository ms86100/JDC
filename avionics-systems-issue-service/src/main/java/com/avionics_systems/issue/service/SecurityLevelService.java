package com.avionics_systems.issue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for managing issue security levels.
 * Delegates security level validation and access checks to avionics-systems-project-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityLevelService {

    private final RestTemplate restTemplate;

    @Value("${project.service.url}")
    private String projectServiceUrl;

    /**
     * Get security levels that the user can assign to issues.
     * Calls avionics-systems-project-service to check user's permission to assign each level.
     */
    public List<SecurityLevelInfo> getAccessibleSecurityLevels(UUID userId, UUID projectId) {
        List<SecurityLevelInfo> accessibleLevels = new ArrayList<>();

        try {
            // Get all security levels for the project
            String url = String.format("%s/api/security-levels/project/%s", projectServiceUrl, projectId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);

            if (response != null) {
                for (Map<String, Object> level : response) {
                    UUID levelId = UUID.fromString(String.valueOf(level.get("id")));
                    if (canUserAccessLevel(userId, levelId, projectId)) {
                        SecurityLevelInfo info = SecurityLevelInfo.builder()
                                .id(levelId)
                                .name((String) level.get("name"))
                                .description((String) level.get("description"))
                                .levelType((String) level.get("levelType"))
                                .build();
                        accessibleLevels.add(info);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get accessible security levels for user {}: {}", userId, e.getMessage());
        }

        return accessibleLevels;
    }

    /**
     * Check if a user can access a specific security level.
     * In production, this would verify the user is a member of the security level.
     */
    public boolean canUserAccessLevel(UUID userId, UUID levelId, UUID projectId) {
        try {
            // First check if user has ADMINISTER_PROJECTS permission
            String permUrl = String.format("%s/api/projects/%s/permissions/check?userId=%s&permission=%s",
                    projectServiceUrl, projectId, userId, "ADMINISTER_PROJECTS");
            @SuppressWarnings("unchecked")
            Map<String, Object> permResponse = restTemplate.getForObject(permUrl, Map.class);
            if (Boolean.TRUE.equals(permResponse != null ? permResponse.get("hasPermission") : null)) {
                return true;
            }

            // Check if user has ASSIGN_ISSUES permission (allows assigning security levels)
            String assignUrl = String.format("%s/api/projects/%s/permissions/check?userId=%s&permission=%s",
                    projectServiceUrl, projectId, userId, "ASSIGN_ISSUES");
            @SuppressWarnings("unchecked")
            Map<String, Object> assignResponse = restTemplate.getForObject(assignUrl, Map.class);
            if (!Boolean.TRUE.equals(assignResponse != null ? assignResponse.get("hasPermission") : null)) {
                return false;
            }

            // Check if user is a member of the security level via SecurityLevelMember
            String memberUrl = String.format("%s/api/security-levels/%s/members/%s/check", projectServiceUrl, levelId, userId);
            @SuppressWarnings("unchecked")
            Map<String, Object> memberResponse = restTemplate.getForObject(memberUrl, Map.class);
            return Boolean.TRUE.equals(memberResponse != null ? memberResponse.get("isMember") : true);

        } catch (Exception e) {
            log.warn("Failed to check level access for user {} level {}: {}", userId, levelId, e.getMessage());
            return false; // Fail-closed for security
        }
    }

    /**
     * Get security level details by ID.
     */
    public Optional<SecurityLevelInfo> getSecurityLevelById(UUID levelId) {
        try {
            String url = String.format("%s/api/security-levels/%s", projectServiceUrl, levelId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                return Optional.of(SecurityLevelInfo.builder()
                        .id(levelId)
                        .name((String) response.get("name"))
                        .description((String) response.get("description"))
                        .levelType((String) response.get("levelType"))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to get security level {}: {}", levelId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Validates that a security level exists and is valid for the project.
     */
    public boolean isValidSecurityLevel(UUID levelId, UUID projectId) {
        try {
            String url = String.format("%s/api/security-levels/%s/project/%s/validate", projectServiceUrl, levelId, projectId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return Boolean.TRUE.equals(response != null ? response.get("valid") : false);
        } catch (Exception e) {
            // If validation endpoint doesn't exist, fall back to basic existence check
            log.debug("Security level validation endpoint not available: {}", e.getMessage());
            return getSecurityLevelById(levelId).isPresent();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SecurityLevelInfo {
        private UUID id;
        private String name;
        private String description;
        private String levelType;
    }
}