package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.UserMapping;
import com.avionics_systems.migration.persister.UserPersisterHandler;
import com.avionics_systems.migration.service.clients.UserServiceClient;
import com.avionics_systems.migration.service.clients.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Resolves source user identifiers via user-service directory lookup (P3-06).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDirectoryMappingService {

    private final UserServiceClient userServiceClient;
    private final UserPersisterHandler userPersisterHandler;

    public List<UserMapping> resolveSourceUsers(Collection<String> sourceIdentifiers, UUID jobId) {
        if (sourceIdentifiers == null || sourceIdentifiers.isEmpty()) {
            return List.of();
        }
        List<UserMapping> results = new ArrayList<>();
        for (String sourceId : new LinkedHashSet<>(sourceIdentifiers)) {
            if (sourceId == null || sourceId.isBlank()) {
                continue;
            }
            UserMapping mapping = resolveOne(sourceId.trim(), jobId);
            if (mapping != null) {
                results.add(mapping);
            }
        }
        return results;
    }

    public UUID resolveToTargetUserId(String sourceIdentifier, UUID jobId) {
        UserMapping mapping = resolveOne(sourceIdentifier, jobId);
        return mapping != null ? mapping.getTargetUserId() : null;
    }

    private UserMapping resolveOne(String sourceId, UUID jobId) {
        try {
            UserMapping matched = userPersisterHandler.autoMatchUsers(List.of(sourceId), jobId).stream()
                    .findFirst()
                    .orElse(null);
            if (matched != null && matched.getTargetUserId() != null) {
                return matched;
            }

            try {
                List<UserResponse> searchHits = userServiceClient.searchUsers(sourceId);
                if (!searchHits.isEmpty()) {
                    UserResponse user = searchHits.getFirst();
                    if (user.getId() != null) {
                        return userPersisterHandler.persistUserMapping(
                                jobId,
                                sourceId,
                                "SEARCH",
                                UUID.fromString(user.getId()),
                                user.getUsername(),
                                "EXACT_MATCH"
                        );
                    }
                }
            } catch (Exception e) {
                log.debug("User search failed for {}: {}", sourceId, e.getMessage());
            }

            if (matched != null && matched.getTargetUserId() != null) {
                return matched;
            }
            return null;
        } catch (Exception e) {
            log.warn("User resolution skipped for {} (import continues without assignee): {}",
                    sourceId, e.getMessage());
            return null;
        }
    }
}
