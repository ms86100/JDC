package com.avionics_systems.migration.service.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.service.clients.dto.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client for interacting with the Issue Link Service.
 * Handles issue relationship CRUD operations (Epic Link, Parent-Child, Blocks, etc.).
 */
@Component
@Slf4j
public class IssueLinkServiceClient extends BaseServiceClient {

    private static final String SERVICE_PATH_PREFIX = "/api/issue-links";

    @Autowired
    public IssueLinkServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            ServiceClientsConfig config) {
        super(restTemplate, objectMapper, circuitBreakerRegistry,
              "issueService", config.getIssueServiceUrl()); // Uses issue service for links
    }

    @Override
    protected String getCircuitBreakerName() {
        return "issueService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH_PREFIX;
    }

    /**
     * Create an issue link between two issues.
     */
    public IssueLinkResponse createIssueLink(CreateIssueLinkRequest request) {
        log.info("Creating issue link: {} {} -> {}",
                request.getSourceIssueId(), request.getLinkType(), request.getTargetIssueId());
        try {
            IssueLinkResponse response = executePost(SERVICE_PATH_PREFIX, request, IssueLinkResponse.class);
            if (response != null && response.isSuccess()) {
                log.debug("Successfully created issue link: {}", response.getId());
                return response;
            } else {
                throw ServiceClientException.serverError(serviceName, 500,
                    "Issue link creation failed: " + (response != null ? response.getErrorMessage() : "Unknown error"),
                    SERVICE_PATH_PREFIX, "POST");
            }
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(serviceName, SERVICE_PATH_PREFIX, e);
        }
    }

    /**
     * Link a story to an epic.
     */
    public void linkStoryToEpic(String storyId, String epicId) {
        log.info("Linking story {} to epic {}", storyId, epicId);
        CreateIssueLinkRequest request = CreateIssueLinkRequest.builder()
                .sourceIssueId(storyId)
                .targetIssueId(epicId)
                .linkType("Epic Link")
                .direction("OUTWARD")
                .build();
        createIssueLink(request);
    }

    /**
     * Link a subtask to its parent issue.
     */
    public void linkSubtaskToParent(String subtaskId, String parentId) {
        log.info("Linking subtask {} to parent {}", subtaskId, parentId);
        CreateIssueLinkRequest request = CreateIssueLinkRequest.builder()
                .sourceIssueId(subtaskId)
                .targetIssueId(parentId)
                .linkType("Parent")
                .direction("OUTWARD")
                .build();
        createIssueLink(request);
    }

    /**
     * Create a blocks relationship.
     */
    public IssueLinkResponse createBlocksLink(String sourceIssueId, String targetIssueId) {
        CreateIssueLinkRequest request = CreateIssueLinkRequest.builder()
                .sourceIssueId(sourceIssueId)
                .targetIssueId(targetIssueId)
                .linkType("Blocks")
                .direction("OUTWARD")
                .build();
        return createIssueLink(request);
    }

    /**
     * Create a "is blocked by" relationship.
     */
    public IssueLinkResponse createBlockedByLink(String sourceIssueId, String targetIssueId) {
        CreateIssueLinkRequest request = CreateIssueLinkRequest.builder()
                .sourceIssueId(sourceIssueId)
                .targetIssueId(targetIssueId)
                .linkType("Is Blocked By")
                .direction("INWARD")
                .build();
        return createIssueLink(request);
    }

    /**
     * Get all links for an issue.
     */
    public List<IssueLinkResponse> getIssueLinks(String issueId) {
        log.debug("Fetching links for issue: {}", issueId);
        try {
            IssueLinkResponse[] response = executeGet(
                SERVICE_PATH_PREFIX + "/issue/" + issueId,
                IssueLinkResponse[].class);
            if (response != null) {
                return List.of(response);
            }
            return List.of();
        } catch (ServiceClientException e) {
            log.warn("Failed to fetch links for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Delete an issue link.
     */
    public void deleteIssueLink(String linkId) {
        log.info("Deleting issue link: {}", linkId);
        executeDelete(SERVICE_PATH_PREFIX + "/" + linkId);
    }

    /**
     * Delete all links for an issue.
     */
    public void deleteIssueLinks(String issueId) {
        log.info("Deleting all links for issue: {}", issueId);
        executeDelete(SERVICE_PATH_PREFIX + "/issue/" + issueId);
    }

    /**
     * Batch create issue links.
     */
    public List<IssueLinkResponse> createIssueLinksBatch(List<CreateIssueLinkRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        log.info("Creating {} issue links in batch", requests.size());
        try {
            IssueLinkResponse[] response = executePost(
                SERVICE_PATH_PREFIX + "/batch",
                requests,
                IssueLinkResponse[].class);
            if (response != null) {
                return List.of(response);
            }
            return List.of();
        } catch (ServiceClientException e) {
            log.error("Batch issue link creation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(serviceName, SERVICE_PATH_PREFIX + "/batch", e);
        }
    }

    /**
     * Request DTO for creating issue links.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateIssueLinkRequest {
        private String sourceIssueId;
        private String targetIssueId;
        private String linkType;
        private String direction; // OUTWARD, INWARD
        private String description;
        private Map<String, Object> properties;
    }

    /**
     * Response DTO for issue link operations.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class IssueLinkResponse {
        private String id;
        private String sourceIssueId;
        private String sourceIssueKey;
        private String targetIssueId;
        private String targetIssueKey;
        private String linkType;
        private String direction;
        private java.time.LocalDateTime created;
        private boolean success;
        private String errorMessage;
    }
}