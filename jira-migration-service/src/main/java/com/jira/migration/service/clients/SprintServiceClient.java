package com.jira.migration.service.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.service.clients.dto.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Service client for the Sprint Service.
 * Provides operations for managing sprints in Agile projects.
 * Features:
 * - Circuit breaker protection
 * - Retry with exponential backoff
 * - Request/Response logging
 * - Auth token propagation
 */
@Service
@Slf4j
public class SprintServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "sprintService";
    private static final String SERVICE_PATH_PREFIX = "/api/sprints";

    @Autowired
    public SprintServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.sprintServiceUrl:http://localhost:8091}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "sprintService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH_PREFIX;
    }

    /**
     * Creates a new sprint.
     *
     * @param request the sprint creation request
     * @return the created sprint response
     */
    public SprintResponse createSprint(CreateSprintRequest request) {
        log.info("Creating sprint: {} for project {}", request.getName(), request.getProjectId());
        try {
            SprintResponse response = executePost(SERVICE_PATH_PREFIX, request, SprintResponse.class);
            if (response != null && response.isSuccess()) {
                log.info("Successfully created sprint: {} with ID: {}", request.getName(), response.getId());
                return response;
            } else {
                throw ServiceClientException.serverError(serviceName, 500,
                    "Sprint creation failed: " + (response != null ? response.getErrorMessage() : "Unknown error"),
                    SERVICE_PATH_PREFIX, "POST");
            }
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(serviceName, SERVICE_PATH_PREFIX, e);
        }
    }

    /**
     * Retrieves a sprint by its ID.
     *
     * @param sprintId the sprint ID
     * @return the sprint response
     */
    public Optional<SprintResponse> getSprint(String sprintId) {
        log.debug("Fetching sprint with ID: {}", sprintId);
        try {
            SprintResponse response = executeGet(SERVICE_PATH_PREFIX + "/" + sprintId, SprintResponse.class);
            return Optional.ofNullable(response);
        } catch (ServiceClientException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Retrieves all sprints for a project.
     *
     * @param projectId the project ID
     * @return list of project sprints
     */
    public List<SprintResponse> getProjectSprints(String projectId) {
        log.debug("Fetching sprints for project: {}", projectId);
        try {
            SprintResponse[] response = executeGet(
                SERVICE_PATH_PREFIX + "/project/" + projectId,
                SprintResponse[].class);
            if (response != null) {
                return List.of(response);
            }
            return List.of();
        } catch (ServiceClientException e) {
            log.warn("Failed to fetch sprints for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieves active sprints for a project.
     *
     * @param projectId the project ID
     * @return list of active sprints
     */
    public Optional<SprintResponse> getActiveSprint(String projectId) {
        log.debug("Fetching active sprint for project: {}", projectId);
        try {
            SprintResponse response = executeGet(
                SERVICE_PATH_PREFIX + "/project/" + projectId + "/active",
                SprintResponse.class);
            return Optional.ofNullable(response);
        } catch (ServiceClientException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Updates an existing sprint.
     *
     * @param sprintId the sprint ID
     * @param request the update request
     * @return the updated sprint response
     */
    public SprintResponse updateSprint(String sprintId, CreateSprintRequest request) {
        log.info("Updating sprint: {}", sprintId);
        try {
            return executePut(SERVICE_PATH_PREFIX + "/" + sprintId, request, SprintResponse.class);
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/" + sprintId, e);
        }
    }

    /**
     * Starts a sprint.
     *
     * @param sprintId the sprint ID
     * @return the started sprint response
     */
    public SprintResponse startSprint(String sprintId) {
        log.info("Starting sprint: {}", sprintId);
        try {
            return executePost(SERVICE_PATH_PREFIX + "/" + sprintId + "/start", null, SprintResponse.class);
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/" + sprintId + "/start", e);
        }
    }

    /**
     * Completes a sprint.
     *
     * @param sprintId the sprint ID
     * @return the completed sprint response
     */
    public SprintResponse completeSprint(String sprintId) {
        log.info("Completing sprint: {}", sprintId);
        try {
            return executePost(SERVICE_PATH_PREFIX + "/" + sprintId + "/complete", null, SprintResponse.class);
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/" + sprintId + "/complete", e);
        }
    }

    /**
     * Deletes a sprint.
     *
     * @param sprintId the sprint ID
     */
    public void deleteSprint(String sprintId) {
        log.info("Deleting sprint: {}", sprintId);
        executeDelete(SERVICE_PATH_PREFIX + "/" + sprintId);
    }

    /**
     * Adds issues to a sprint.
     *
     * @param sprintId the sprint ID
     * @param issueIds list of issue IDs to add
     * @return the updated sprint response
     */
    public SprintResponse addIssuesToSprint(String sprintId, List<String> issueIds) {
        log.info("Adding {} issues to sprint: {}", issueIds.size(), sprintId);
        try {
            return executePut(SERVICE_PATH_PREFIX + "/" + sprintId + "/issues", issueIds, SprintResponse.class);
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/" + sprintId + "/issues", e);
        }
    }

    /**
     * Removes issues from a sprint.
     *
     * @param sprintId the sprint ID
     * @param issueIds list of issue IDs to remove
     * @return the updated sprint response
     */
    public SprintResponse removeIssuesFromSprint(String sprintId, List<String> issueIds) {
        log.info("Removing {} issues from sprint: {}", issueIds.size(), sprintId);
        try {
            return executePut(SERVICE_PATH_PREFIX + "/" + sprintId + "/issues/remove", issueIds, SprintResponse.class);
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/" + sprintId + "/issues/remove", e);
        }
    }

    /**
     * Cancels a sprint.
     *
     * @param sprintId the sprint ID
     * @return the cancelled sprint response
     */
    public SprintResponse cancelSprint(String sprintId) {
        log.info("Cancelling sprint: {}", sprintId);
        try {
            return executePost(SERVICE_PATH_PREFIX + "/" + sprintId + "/cancel", null, SprintResponse.class);
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/" + sprintId + "/cancel", e);
        }
    }
}