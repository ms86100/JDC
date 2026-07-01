package com.jira.migration.persister;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.UserMapping;
import com.jira.migration.exception.*;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.UserMappingRepository;
import com.jira.migration.service.clients.*;
import com.jira.migration.service.clients.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * User Persister Handler
 * Handles user mapping and creation during import using real user service calls.
 * Supports EXACT_MATCH, EMAIL_MATCH, CREATE_NEW mapping strategies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserPersisterHandler {

    private final UserMappingRepository userMappingRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final UserServiceClient userServiceClient;

    // Mapping confidence thresholds
    private static final double HIGH_CONFIDENCE = 95.0;
    private static final double MEDIUM_CONFIDENCE = 70.0;

    @Transactional(rollbackFor = Exception.class)
    public UserMapping persistUserMapping(
            UUID jobId,
            String sourceIdentifier,
            String sourceType,
            UUID targetUserId,
            String targetUsername,
            String mappingType) {

        UserMapping mapping = UserMapping.builder()
                .jobId(jobId)
                .sourceIdentifier(sourceIdentifier)
                .sourceType(sourceType)
                .targetUserId(targetUserId)
                .targetUsername(targetUsername)
                .mappingType(mappingType)
                .confidenceScore(calculateConfidence(mappingType))
                .build();

        return userMappingRepository.save(mapping);
    }

    /**
     * Resolve user by email, creating if not found.
     * Returns the resolved user ID or null if resolution failed.
     */
    public String resolveOrCreateUser(String email, String displayName, UUID jobId) {
        if (email == null || email.isBlank()) {
            log.warn("Cannot resolve user: email is required");
            return null;
        }

        // Check if already mapped
        Optional<UserMapping> existing = userMappingRepository.findByJobIdAndSourceIdentifier(jobId, email);
        if (existing.isPresent() && existing.get().getTargetUserId() != null) {
            log.debug("User {} already mapped to {}", email, existing.get().getTargetUserId());
            return existing.get().getTargetUserId().toString();
        }

        // Try to find existing user by email
        try {
            UserResponse existingUser = userServiceClient.lookupUserByEmail(email).orElse(null);
            if (existingUser != null && existingUser.getId() != null) {
                // Map to existing user
                UserMapping mapping = persistUserMapping(
                        jobId, email, "EMAIL",
                        UUID.fromString(existingUser.getId()),
                        existingUser.getUsername(),
                        "EMAIL_MATCH"
                );
                return existingUser.getId();
            }
        } catch (Exception e) {
            log.debug("User lookup failed for email {}: {}", email, e.getMessage());
        }

        try {
            Map<String, Object> createPayload = new HashMap<>();
            createPayload.put("email", email);
            createPayload.put("username", email.contains("@") ? email.substring(0, email.indexOf('@')) : email);
            createPayload.put("displayName", displayName != null ? displayName : email);
            UserResponse created = userServiceClient.tryCreateUser(createPayload).orElse(null);
            if (created != null && created.getId() != null) {
                persistUserMapping(jobId, email, "EMAIL",
                        UUID.fromString(created.getId()), created.getUsername(), "CREATE_NEW");
                return created.getId();
            }
        } catch (Exception e) {
            log.warn("User create failed for {}: {}", email, e.getMessage());
        }
        return null;
    }

    public int importUsersFromProject(UUID jobId, UUID sourceProjectId) {
        int count = 0;
        try {
            for (UserResponse user : userServiceClient.getUsersByProject(sourceProjectId.toString())) {
                if (user.getEmail() != null) {
                    String id = resolveOrCreateUser(user.getEmail(), user.getDisplayName(), jobId);
                    if (id != null) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Project user import failed: {}", e.getMessage());
        }
        return count;
    }

    /**
     * Auto-match users based on email or username.
     */
    public List<UserMapping> autoMatchUsers(List<String> sourceUserIds, UUID jobId) {
        List<UserMapping> mappings = new ArrayList<>();

        for (String sourceId : sourceUserIds) {
            UserMapping mapping = tryMatchUser(sourceId, jobId);
            if (mapping != null) {
                mappings.add(mapping);
            }
        }

        return mappings;
    }

    private UserMapping tryMatchUser(String sourceId, UUID jobId) {
        // Check if already mapped
        Optional<UserMapping> existing = userMappingRepository.findByJobIdAndSourceIdentifier(jobId, sourceId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Try email match
        if (sourceId.contains("@")) {
            Optional<UserResponse> userOpt = userServiceClient.lookupUserByEmail(sourceId);
            if (userOpt.isPresent() && userOpt.get().getId() != null) {
                UserResponse user = userOpt.get();
                return persistUserMapping(
                        jobId, sourceId, "EMAIL",
                        UUID.fromString(user.getId()),
                        user.getUsername(),
                        "EMAIL_MATCH"
                );
            }
        }

        // Try username match
        Optional<UserResponse> userOpt = userServiceClient.lookupUserByUsername(sourceId);
        if (userOpt.isPresent() && userOpt.get().getId() != null) {
            UserResponse user = userOpt.get();
            return persistUserMapping(
                    jobId, sourceId, "USERNAME",
                    UUID.fromString(user.getId()),
                    user.getUsername(),
                    "EXACT_MATCH"
            );
        }

        // Create manual mapping (no match found)
        return UserMapping.builder()
                .jobId(jobId)
                .sourceIdentifier(sourceId)
                .sourceType("JIRA_DC")
                .mappingType("UNMATCHED")
                .confidenceScore(0.0)
                .build();
    }

    private double calculateConfidence(String mappingType) {
        return switch (mappingType.toUpperCase()) {
            case "EXACT_MATCH" -> HIGH_CONFIDENCE;
            case "EMAIL_MATCH" -> HIGH_CONFIDENCE - 5;
            case "CREATE_NEW" -> 100.0;
            case "MANUAL" -> 100.0;
            case "UNMATCHED" -> 0.0;
            default -> MEDIUM_CONFIDENCE;
        };
    }

    /**
     * Resolve user reference for an entity.
     */
    public UUID resolveUser(Map<String, Object> entityData, String fieldName, UUID jobId) {
        Object userRef = entityData.get(fieldName);
        if (userRef == null) return null;

        String identifier = userRef.toString();

        Optional<UserMapping> mapping = userMappingRepository
                .findByJobIdAndSourceIdentifier(jobId, identifier);

        return mapping.map(UserMapping::getTargetUserId).orElse(null);
    }

    /**
     * Validate all user references in a batch.
     */
    public List<String> validateUserReferences(List<String> userIds, UUID jobId) {
        List<String> errors = new ArrayList<>();

        for (String userId : userIds) {
            Optional<UserMapping> mapping = userMappingRepository
                    .findByJobIdAndSourceIdentifier(jobId, userId);

            if (mapping.isEmpty()) {
                errors.add("Unknown user: " + userId);
            } else if (mapping.get().getTargetUserId() == null) {
                errors.add("Unmapped user: " + userId);
            }
        }

        return errors;
    }

    }