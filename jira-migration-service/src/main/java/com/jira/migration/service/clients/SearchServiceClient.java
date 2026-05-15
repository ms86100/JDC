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
 * Service client for the Search Service.
 * Provides operations for indexing and searching entities.
 */
@Service
@Slf4j
public class SearchServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "searchService";
    private static final String SERVICE_PATH = "/search";

    @Autowired
    public SearchServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.searchServiceUrl:http://localhost:8085}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "searchService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH;
    }

    /**
     * Indexes an entity for search.
     *
     * @param entityType the type of entity (issue, project, user, etc.)
     * @param entityId the entity ID
     * @param title the title for the indexed document
     * @param content the content to index
     */
    public void indexEntity(String entityType, String entityId, String title, String content) {
        log.info("Indexing {} entity: {}", entityType, entityId);
        String endpoint = SERVICE_PATH + "/index";

        IndexEntityRequest request = IndexEntityRequest.builder()
                .entityType(entityType)
                .entityId(entityId)
                .title(title)
                .content(content)
                .build();

        executePost(endpoint, request, Void.class);
    }

    /**
     * Indexes an entity with additional metadata.
     *
     * @param request the index entity request with full metadata
     */
    public void indexEntity(IndexEntityRequest request) {
        log.info("Indexing {} entity: {} with metadata", request.getEntityType(), request.getEntityId());
        String endpoint = SERVICE_PATH + "/index";
        executePost(endpoint, request, Void.class);
    }

    /**
     * Removes an entity from the search index.
     *
     * @param entityType the type of entity
     * @param entityId the entity ID
     */
    public void removeFromIndex(String entityType, String entityId) {
        log.info("Removing {} entity {} from index", entityType, entityId);
        String endpoint = SERVICE_PATH + "/index/" + entityType + "/" + entityId;
        executeDelete(endpoint);
    }

    /**
     * Triggers a full reindex of all entities of a specific type.
     *
     * @param entityType the type of entity to reindex
     * @return the reindex status response
     */
    public ReindexStatusResponse reindexAll(String entityType) {
        log.info("Triggering full reindex for entity type: {}", entityType);
        String endpoint = SERVICE_PATH + "/reindex";

        ReindexStatusResponse response = executePost(endpoint, entityType, ReindexStatusResponse.class);
        return response;
    }

    /**
     * Triggers a full reindex with all entity types.
     *
     * @return the reindex status response
     */
    public ReindexStatusResponse reindexAll() {
        log.info("Triggering full system reindex");
        String endpoint = SERVICE_PATH + "/reindex-all";

        ReindexStatusResponse response = executePost(endpoint, null, ReindexStatusResponse.class);
        return response;
    }

    /**
     * Gets the current status of the search index.
     *
     * @return the index status response
     */
    public IndexStatusResponse getIndexStatus() {
        log.debug("Fetching search index status");
        String endpoint = SERVICE_PATH + "/status";
        return executeGet(endpoint, IndexStatusResponse.class);
    }

    /**
     * Searches for entities matching a query.
     *
     * @param query the search query
     * @param entityType optional entity type filter
     * @param projectId optional project filter
     * @param limit maximum number of results
     * @return list of search results
     */
    public List<SearchResult> search(String query, String entityType, String projectId, int limit) {
        log.debug("Searching for: {} (type={}, project={}, limit={})", query, entityType, projectId, limit);
        String endpoint = SERVICE_PATH + "/query?q=" + encodeValue(query);

        if (entityType != null) {
            endpoint += "&type=" + entityType;
        }
        if (projectId != null) {
            endpoint += "&project=" + projectId;
        }
        endpoint += "&limit=" + limit;

        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<SearchResult>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity,
                    new ParameterizedTypeReference<List<SearchResult>>() {});
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
     * Gets the reindex status for a specific entity type.
     *
     * @param entityType the entity type
     * @return the reindex status response
     */
    public ReindexStatusResponse getReindexStatus(String entityType) {
        log.debug("Fetching reindex status for: {}", entityType);
        String endpoint = SERVICE_PATH + "/reindex/status/" + entityType;
        return executeGet(endpoint, ReindexStatusResponse.class);
    }

    /**
     * Inner class representing a search result.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchResult {
        private String id;
        private String type;
        private String title;
        private String content;
        private String projectId;
        private double score;
        private List<String> highlights;
    }

    private String encodeValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}