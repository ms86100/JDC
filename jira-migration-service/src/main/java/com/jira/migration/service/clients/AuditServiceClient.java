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
 * Service client for the Audit Service.
 * Provides operations for logging and retrieving audit records.
 */
@Service
@Slf4j
public class AuditServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "auditService";
    private static final String AUDIT_LOGS_PATH = "/api/audit/logs";

    @Autowired
    public AuditServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.auditServiceUrl:http://localhost:8090}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "auditService";
    }

    @Override
    protected String getServicePathPrefix() {
        return AUDIT_LOGS_PATH;
    }

    /**
     * Creates a new audit log entry.
     *
     * @param request the audit log creation request
     * @return the audit log response
     */
    public AuditLogResponse createAuditLog(CreateAuditLogRequest request) {
        log.info("Creating audit log for {} {}: {}",
                request.getEntityType(), request.getEntityId(), request.getAction());
        return executePost(AUDIT_LOGS_PATH, request, AuditLogResponse.class);
    }

    /**
     * Retrieves all audit logs with pagination.
     *
     * @param page page number (0-based)
     * @param size page size
     * @return paginated audit log response
     */
    public PagedAuditLogResponse getAuditLogs(int page, int size) {
        log.debug("Fetching audit logs (page={}, size={})", page, size);

        String endpoint = AUDIT_LOGS_PATH + "?page=" + page + "&size=" + size;
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<PagedAuditLogResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, PagedAuditLogResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Get audit logs {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Get audit logs {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Retrieves audit logs for a specific entity.
     *
     * @param entityType the entity type (issue, project, user, etc.)
     * @param entityId the entity ID
     * @return list of audit log responses
     */
    public List<AuditLogResponse> getEntityAuditLogs(String entityType, String entityId) {
        log.debug("Fetching audit logs for {}: {}", entityType, entityId);

        ParameterizedTypeReference<List<AuditLogResponse>> typeRef =
            new ParameterizedTypeReference<List<AuditLogResponse>>() {};

        String endpoint = AUDIT_LOGS_PATH + "/" + entityType + "/" + entityId;
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<AuditLogResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Entity audit logs {} -> {} ({}ms), found {} logs",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Entity audit logs {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Retrieves audit logs for a specific user with pagination.
     *
     * @param userId the user ID
     * @param page page number (0-based)
     * @param size page size
     * @return paginated audit log response
     */
    public PagedAuditLogResponse getUserAuditLogs(String userId, int page, int size) {
        log.debug("Fetching audit logs for user: {} (page={}, size={})", userId, page, size);

        String endpoint = AUDIT_LOGS_PATH + "/user/" + userId + "?page=" + page + "&size=" + size;
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<PagedAuditLogResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, PagedAuditLogResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("User audit logs {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("User audit logs {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Searches audit logs by action type.
     *
     * @param action the action type
     * @param page page number
     * @param size page size
     * @return paginated audit log response
     */
    public PagedAuditLogResponse searchByAction(String action, int page, int size) {
        log.debug("Searching audit logs by action: {} (page={}, size={})", action, page, size);

        String endpoint = AUDIT_LOGS_PATH + "/search?action=" + action + "&page=" + page + "&size=" + size;
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<PagedAuditLogResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, PagedAuditLogResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Search audit logs by action {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Search audit logs by action {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Searches audit logs by category.
     *
     * @param category the category
     * @param page page number
     * @param size page size
     * @return paginated audit log response
     */
    public PagedAuditLogResponse searchByCategory(String category, int page, int size) {
        log.debug("Searching audit logs by category: {} (page={}, size={})", category, page, size);

        String endpoint = AUDIT_LOGS_PATH + "/category/" + category + "?page=" + page + "&size=" + size;
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<PagedAuditLogResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, PagedAuditLogResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Search audit logs by category {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Search audit logs by category {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Gets audit logs within a date range.
     *
     * @param startDate start date (ISO format)
     * @param endDate end date (ISO format)
     * @param page page number
     * @param size page size
     * @return paginated audit log response
     */
    public PagedAuditLogResponse getLogsByDateRange(String startDate, String endDate, int page, int size) {
        log.debug("Fetching audit logs by date range: {} to {} (page={}, size={})",
                startDate, endDate, page, size);

        String endpoint = AUDIT_LOGS_PATH + "/range?start=" + startDate + "&end=" + endDate
                + "&page=" + page + "&size=" + size;
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<PagedAuditLogResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, PagedAuditLogResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Audit logs by date range {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Audit logs by date range {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Logs a migration action for audit purposes.
     *
     * @param userId the user performing the migration
     * @param projectId the project being migrated
     * @param action the specific action (EXPORT, IMPORT, VALIDATE, etc.)
     * @param details additional details about the action
     */
    public void logMigrationAction(String userId, String projectId, String action, String details) {
        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .entityType("MIGRATION")
                .entityId(projectId)
                .action(action)
                .userId(userId)
                .description(details)
                .category("MIGRATION")
                .source("JIRA_MIGRATION_SERVICE")
                .build();
        createAuditLog(request);
    }

    /**
     * Logs a batch operation for audit purposes.
     *
     * @param userId the user performing the operation
     * @param operation the batch operation type
     * @param entityType the type of entities being operated on
     * @param totalCount total number of entities
     * @param successCount number of successful operations
     * @param failureCount number of failed operations
     */
    public void logBatchOperation(String userId, String operation, String entityType,
                                   int totalCount, int successCount, int failureCount) {
        String details = String.format("Batch %s: %d %s processed (%d success, %d failed)",
                operation, totalCount, entityType, successCount, failureCount);

        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .entityType(entityType)
                .entityId("BATCH_" + System.currentTimeMillis())
                .action("BATCH_" + operation.toUpperCase())
                .userId(userId)
                .description(details)
                .category("BATCH_OPERATION")
                .source("JIRA_MIGRATION_SERVICE")
                .build();
        createAuditLog(request);
    }
}