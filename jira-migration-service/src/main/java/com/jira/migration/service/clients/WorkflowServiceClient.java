package com.jira.migration.service.clients;

import com.jira.migration.service.clients.dto.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * Service client for the Workflow Service.
 * Provides operations for managing workflows.
 */
@Service
@Slf4j
public class WorkflowServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "workflowService";
    private static final String SERVICE_PATH = "/api/workflows";

    @Autowired
    public WorkflowServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.workflowServiceUrl:http://localhost:8084}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "workflowService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH;
    }

    /**
     * Retrieves a workflow by its ID.
     *
     * @param workflowId the workflow ID
     * @return the workflow response
     */
    public WorkflowResponse getWorkflow(String workflowId) {
        log.debug("Fetching workflow with ID: {}", workflowId);
        String endpoint = SERVICE_PATH + "/" + workflowId;
        return executeGet(endpoint, WorkflowResponse.class);
    }

    /**
     * Retrieves all workflows.
     *
     * @return list of all workflow responses
     */
    public List<WorkflowResponse> getAllWorkflows() {
        log.debug("Fetching all workflows");

        ParameterizedTypeReference<List<WorkflowResponse>> typeRef =
            new ParameterizedTypeReference<List<WorkflowResponse>>() {};

        String url = buildUrl(SERVICE_PATH);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<WorkflowResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("GET all workflows {} -> {} ({}ms), found {} workflows",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("GET all workflows {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, SERVICE_PATH, e);
        }
    }

    /**
     * Retrieves the workflow associated with a project.
     *
     * @param projectId the project ID
     * @return the project workflow response
     */
    public WorkflowResponse getProjectWorkflow(String projectId) {
        log.debug("Fetching workflow for project: {}", projectId);
        String endpoint = SERVICE_PATH + "/project/" + projectId;
        return executeGet(endpoint, WorkflowResponse.class);
    }

    /**
     * Creates a new workflow.
     *
     * @param request the workflow creation request
     * @return the created workflow response
     */
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        log.info("Creating workflow: {}", request.getName());
        return executePost(SERVICE_PATH, request, WorkflowResponse.class);
    }

    /**
     * Updates an existing workflow.
     *
     * @param workflowId the workflow ID
     * @param request the update request
     * @return the updated workflow response
     */
    public WorkflowResponse updateWorkflow(String workflowId, CreateWorkflowRequest request) {
        log.info("Updating workflow: {}", workflowId);
        String endpoint = SERVICE_PATH + "/" + workflowId;
        return executePut(endpoint, request, WorkflowResponse.class);
    }

    /**
     * Activates a workflow.
     *
     * @param workflowId the workflow ID
     * @return the activated workflow response
     */
    public WorkflowResponse activateWorkflow(String workflowId) {
        log.info("Activating workflow: {}", workflowId);
        String endpoint = SERVICE_PATH + "/" + workflowId + "/activate";

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = buildUrl(endpoint);
        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<WorkflowResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, WorkflowResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Activate workflow {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Activate workflow {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Deactivates a workflow.
     *
     * @param workflowId the workflow ID
     * @return the deactivated workflow response
     */
    public WorkflowResponse deactivateWorkflow(String workflowId) {
        log.info("Deactivating workflow: {}", workflowId);
        String endpoint = SERVICE_PATH + "/" + workflowId + "/deactivate";

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = buildUrl(endpoint);
        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<WorkflowResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, WorkflowResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Deactivate workflow {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Deactivate workflow {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Deletes a workflow.
     *
     * @param workflowId the workflow ID
     */
    public void deleteWorkflow(String workflowId) {
        log.info("Deleting workflow: {}", workflowId);
        String endpoint = SERVICE_PATH + "/" + workflowId;
        executeDelete(endpoint);
    }

    public WorkflowResponse importWorkflowDescriptor(ImportWorkflowDescriptorRequest request) {
        log.info("Importing workflow descriptor: {}", request.getName());
        return executePost(SERVICE_PATH + "/import/descriptor", request, WorkflowResponse.class);
    }
}