package com.jira.migration.service.clients;

import com.jira.migration.service.IssueServicePayloadMapper;
import com.jira.migration.service.clients.dto.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * Service client for the Issue Service.
 * Provides operations for creating, updating, searching, and managing issues.
 */
@Service
@Slf4j
public class IssueServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "issueService";
    private static final String SERVICE_PATH = "/api/issues";

    private final IssueServicePayloadMapper payloadMapper;

    @Autowired
    public IssueServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            IssueServicePayloadMapper payloadMapper,
            @Value("${services.issueServiceUrl:http://localhost:8084}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
        this.payloadMapper = payloadMapper;
    }

    @Override
    protected String getCircuitBreakerName() {
        return "issueService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH;
    }

    /**
     * Creates a new issue in the system.
     *
     * @param request the issue creation request
     * @return the created issue response
     */
    public IssueResponse createIssue(CreateIssueRequest request) {
        log.info("Creating issue in project {} with type {}", request.getProjectId(), request.getIssueType());
        Map<String, Object> payload = payloadMapper.toIssueServicePayload(request);
        return executePost(SERVICE_PATH, payload, IssueResponse.class);
    }

    public List<JsonNode> listIssueTypes() {
        ParameterizedTypeReference<List<JsonNode>> typeRef = new ParameterizedTypeReference<>() {};
        String url = buildUrl(SERVICE_PATH + "/types");
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<List<JsonNode>> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, typeRef);
        return response.getBody() != null ? response.getBody() : List.of();
    }

    public List<JsonNode> listPriorities() {
        ParameterizedTypeReference<List<JsonNode>> typeRef = new ParameterizedTypeReference<>() {};
        String url = buildUrl(SERVICE_PATH + "/priorities");
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<List<JsonNode>> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, typeRef);
        return response.getBody() != null ? response.getBody() : List.of();
    }

    /**
     * Creates multiple issues in a batch operation.
     *
     * @param requests list of issue creation requests
     * @return list of created issue responses
     */
    public List<IssueResponse> createIssuesBatch(List<CreateIssueRequest> requests) {
        log.info("Batch creating {} issues", requests.size());
        List<Map<String, Object>> payloads = new ArrayList<>();
        for (CreateIssueRequest request : requests) {
            payloads.add(payloadMapper.toIssueServicePayload(request));
        }
        ParameterizedTypeReference<List<IssueResponse>> typeRef =
                new ParameterizedTypeReference<List<IssueResponse>>() {};
        String url = buildUrl(SERVICE_PATH + "/batch");
        HttpHeaders headers = createHeaders();
        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(payloads, headers);
        try {
            ResponseEntity<List<IssueResponse>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, typeRef);
            if (response.getBody() != null) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("Batch endpoint failed, falling back to sequential create: {}", e.getMessage());
        }
        List<IssueResponse> created = new ArrayList<>();
        for (CreateIssueRequest request : requests) {
            created.add(createIssue(request));
        }
        return created;
    }

    /**
     * Retrieves an issue by its ID.
     *
     * @param issueId the issue ID
     * @return the issue response
     */
    public IssueResponse getIssue(String issueId) {
        log.debug("Fetching issue with ID: {}", issueId);
        String endpoint = SERVICE_PATH + "/" + issueId;
        return executeGet(endpoint, IssueResponse.class);
    }

    /**
     * Retrieves an issue by its key.
     *
     * @param issueKey the issue key
     * @return Optional containing the issue response if found
     */
    public Optional<IssueResponse> getIssueByKey(String issueKey) {
        log.debug("Fetching issue with key: {}", issueKey);
        try {
            String endpoint = SERVICE_PATH + "/key/" + encodeValue(issueKey);
            IssueResponse response = executeGet(endpoint, IssueResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.debug("Issue not found for key {}: {}", issueKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Updates an existing issue.
     *
     * @param issueId the issue ID
     * @param request the update request
     * @return the updated issue response
     */
    public IssueResponse updateIssue(String issueId, UpdateIssueRequest request) {
        log.info("Updating issue: {}", issueId);
        String endpoint = SERVICE_PATH + "/" + issueId;
        return executePut(endpoint, request, IssueResponse.class);
    }

    /**
     * Searches for issues using JQL query.
     *
     * @param jql the JQL query string
     * @return list of matching issues
     */
    public List<IssueResponse> searchIssues(String jql) {
        log.debug("Searching issues with JQL: {}", jql);

        ParameterizedTypeReference<List<IssueResponse>> typeRef =
            new ParameterizedTypeReference<List<IssueResponse>>() {};

        String endpoint = SERVICE_PATH + "/search?jql=" + encodeValue(jql);
        String url = buildUrl(endpoint);
        log.debug("GET search request to: {}", url);

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<IssueResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Search {} -> {} ({}ms), found {} results",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Search {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Retrieves all issues for a specific project.
     *
     * @param projectId the project ID
     * @return list of project issues
     */
    public List<IssueResponse> getProjectIssues(String projectId) {
        log.debug("Fetching issues for project: {}", projectId);

        ParameterizedTypeReference<List<IssueResponse>> typeRef =
            new ParameterizedTypeReference<List<IssueResponse>>() {};

        String endpoint = "/api/projects/" + projectId + "/issues";
        String url = buildUrl(endpoint);
        log.debug("GET project issues request to: {}", url);

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<IssueResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Project issues {} -> {} ({}ms), found {} issues",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Project issues {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Deletes an issue.
     *
     * @param issueId the issue ID
     */
    public void deleteIssue(String issueId) {
        log.info("Deleting issue: {}", issueId);
        String endpoint = SERVICE_PATH + "/" + issueId;
        executeDelete(endpoint);
    }

    /**
     * Records change history for migration replay (no workflow transition).
     */
    public void recordChangeHistory(String issueId, Map<String, Object> request) {
        String endpoint = SERVICE_PATH + "/" + issueId + "/history/internal";
        HttpHeaders headers = createHeaders();
        headers.set("X-Workflow-Internal", "migration");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        String url = buildUrl(endpoint);
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (RestClientException e) {
            log.warn("Change history record failed for issue {}: {}", issueId, e.getMessage());
        }
    }

    /**
     * Creates a worklog on an issue (DC migration path).
     */
    public Map<String, Object> createWorklog(String issueId, Map<String, Object> request) {
        String endpoint = SERVICE_PATH + "/" + issueId + "/worklogs";
        HttpHeaders headers = createHeaders();
        headers.set("X-Migration-Import", "true");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        String url = buildUrl(endpoint);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        return response.getBody() != null ? response.getBody() : Map.of();
    }

    /**
     * Creates a project component.
     */
    public Map<String, Object> createComponent(Map<String, Object> request) {
        HttpHeaders headers = createHeaders();
        headers.set("X-Migration-Import", "true");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        String url = buildUrl("/api/components");
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        return response.getBody() != null ? response.getBody() : Map.of();
    }

    /**
     * Creates a project version.
     */
    public Map<String, Object> createVersion(Map<String, Object> request) {
        HttpHeaders headers = createHeaders();
        headers.set("X-Migration-Import", "true");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        String url = buildUrl("/api/versions");
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        return response.getBody() != null ? response.getBody() : Map.of();
    }

    /**
     * Adds a label to an issue.
     */
    public void addIssueLabel(String issueId, String labelName) {
        String endpoint = SERVICE_PATH + "/" + issueId + "/labels";
        Map<String, Object> body = Map.of("name", labelName);
        HttpHeaders headers = createHeaders();
        headers.set("X-Migration-Import", "true");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = buildUrl(endpoint);
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (RestClientException e) {
            log.warn("Add label failed for issue {}: {}", issueId, e.getMessage());
        }
    }

    /**
     * Adds a watcher (migration uses internal header; user id may be username placeholder).
     */
    public void watchIssue(String issueId) {
        String endpoint = SERVICE_PATH + "/" + issueId + "/watch";
        HttpHeaders headers = createHeaders();
        headers.set("X-Migration-Import", "true");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = buildUrl(endpoint);
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (RestClientException e) {
            log.debug("Watch issue skipped for {}: {}", issueId, e.getMessage());
        }
    }

    /**
     * Transitions an issue to a new status.
     *
     * @param issueId the issue ID
     * @param transitionId the transition ID to execute
     * @return the updated issue response
     */
    public IssueResponse transitionIssue(String issueId, String transitionId) {
        log.info("Transitioning issue {} with transition {}", issueId, transitionId);
        String endpoint = SERVICE_PATH + "/" + issueId + "/transitions/" + transitionId;

        Map<String, String> body = new HashMap<>();
        body.put("transitionId", transitionId);

        return executePost(endpoint, body, IssueResponse.class);
    }

    /**
     * Assigns an issue to a user.
     *
     * @param issueId the issue ID
     * @param assigneeId the user ID to assign
     * @return the updated issue response
     */
    public IssueResponse assignIssue(String issueId, String assigneeId) {
        log.info("Assigning issue {} to user {}", issueId, assigneeId);
        String endpoint = SERVICE_PATH + "/" + issueId + "/assignee";

        Map<String, String> body = new HashMap<>();
        body.put("assigneeId", assigneeId);

        return executePut(endpoint, body, IssueResponse.class);
    }

    private String encodeValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}