package com.jira.migration.service.clients;

import com.jira.migration.service.clients.dto.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.web.client.RestClientException;

/**
 * Service client for the Project Service.
 * Provides operations for creating, updating, and managing projects.
 */
@Service
@Slf4j
public class ProjectServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "projectService";
    private static final String SERVICE_PATH = "/api/projects";

    @Autowired
    public ProjectServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.projectServiceUrl:http://localhost:8082}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "projectService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH;
    }

    /**
     * Creates a new project in the system.
     *
     * @param request the project creation request
     * @return the created project response
     */
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.info("Creating project with key: {}", request.getKey());
        return executePost(SERVICE_PATH, request, ProjectResponse.class);
    }

    /**
     * Retrieves a project by its ID.
     *
     * @param projectId the project ID
     * @return the project response
     */
    public ProjectResponse getProject(String projectId) {
        log.debug("Fetching project with ID: {}", projectId);
        String endpoint = SERVICE_PATH + "/" + projectId;
        return executeGet(endpoint, ProjectResponse.class);
    }

    /**
     * Retrieves a project by its key.
     *
     * @param projectKey the project key
     * @return the project response
     */
    public ProjectResponse getProjectByKey(String projectKey) {
        log.debug("Fetching project with key: {}", projectKey);
        String endpoint = SERVICE_PATH + "/key/" + projectKey;
        return executeGet(endpoint, ProjectResponse.class);
    }

    /**
     * Retrieves all projects.
     *
     * @return list of all project responses
     */
    public List<ProjectResponse> getAllProjects() {
        log.debug("Fetching all projects");

        ParameterizedTypeReference<List<ProjectResponse>> typeRef =
            new ParameterizedTypeReference<List<ProjectResponse>>() {};

        String url = buildUrl(SERVICE_PATH);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<ProjectResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("GET all projects {} -> {} ({}ms), found {} projects",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("GET all projects {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, SERVICE_PATH, e);
        }
    }

    /**
     * Updates an existing project.
     *
     * @param projectId the project ID
     * @param request the update request
     * @return the updated project response
     */
    public ProjectResponse updateProject(String projectId, UpdateProjectRequest request) {
        log.info("Updating project: {}", projectId);
        String endpoint = SERVICE_PATH + "/" + projectId;
        return executePut(endpoint, request, ProjectResponse.class);
    }

    /**
     * Deletes a project.
     *
     * @param projectId the project ID
     */
    public void deleteProject(String projectId) {
        log.info("Deleting project: {}", projectId);
        String endpoint = SERVICE_PATH + "/" + projectId;
        executeDelete(endpoint);
    }

    /**
     * Archives a project.
     *
     * @param projectId the project ID
     * @return the archived project response
     */
    public ProjectResponse archiveProject(String projectId) {
        log.info("Archiving project: {}", projectId);
        String endpoint = SERVICE_PATH + "/" + projectId + "/archive";

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = buildUrl(endpoint);
        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<ProjectResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, ProjectResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Archive project {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Archive project {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Restores an archived project.
     *
     * @param projectId the project ID
     * @return the restored project response
     */
    public ProjectResponse restoreProject(String projectId) {
        log.info("Restoring project: {}", projectId);
        String endpoint = SERVICE_PATH + "/" + projectId + "/restore";

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = buildUrl(endpoint);
        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<ProjectResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, ProjectResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Restore project {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Restore project {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Gets a project optionally by ID or key.
     *
     * @param projectIdOrKey the project ID or key
     * @return the project response or empty if not found
     */
    public Optional<ProjectResponse> findProject(String projectIdOrKey) {
        try {
            // Try by ID first
            return Optional.ofNullable(getProject(projectIdOrKey));
        } catch (ServiceClientException e) {
            try {
                // Try by key
                return Optional.ofNullable(getProjectByKey(projectIdOrKey));
            } catch (ServiceClientException e2) {
                return Optional.empty();
            }
        }
    }
}