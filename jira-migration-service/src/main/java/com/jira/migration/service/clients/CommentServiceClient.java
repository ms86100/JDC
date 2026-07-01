package com.jira.migration.service.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.service.clients.dto.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service client for the Comment Service.
 * Provides operations for managing comments on issues.
 * Features:
 * - Circuit breaker protection
 * - Retry with exponential backoff
 * - Request/Response logging
 * - Auth token propagation
 */
@Service
@Slf4j
public class CommentServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "commentService";
    private static final String SERVICE_PATH_PREFIX = "/api/comments";

    @Autowired
    public CommentServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.commentServiceUrl:http://localhost:8086}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "commentService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH_PREFIX;
    }

    /**
     * Adds a comment to an issue.
     *
     * @param issueId the issue ID
     * @param request the comment creation request
     * @return the created comment response
     */
    public CommentResponse addComment(String issueId, CreateCommentRequest request) {
        log.info("Adding comment to issue: {}", issueId);
        try {
            CommentResponse response = executePost(
                SERVICE_PATH_PREFIX + "/issue/" + issueId,
                request,
                CommentResponse.class);

            if (response != null && response.isSuccess()) {
                log.debug("Successfully added comment {} to issue {}", response.getId(), issueId);
                return response;
            } else {
                throw ServiceClientException.serverError(serviceName, 500,
                    "Comment creation failed: " + (response != null ? response.getErrorMessage() : "Unknown error"),
                    SERVICE_PATH_PREFIX, "POST");
            }
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/issue/" + issueId, e);
        }
    }

    /**
     * Retrieves all comments for an issue.
     *
     * @param issueId the issue ID
     * @return list of comment responses
     */
    public List<CommentResponse> getIssueComments(String issueId) {
        log.debug("Fetching comments for issue: {}", issueId);
        try {
            CommentResponse[] response = executeGet(
                SERVICE_PATH_PREFIX + "/issue/" + issueId,
                CommentResponse[].class);

            if (response != null) {
                return Arrays.asList(response);
            }
            return Collections.emptyList();
        } catch (ServiceClientException e) {
            if (e.getStatusCode() == 404) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    /**
     * Retrieves a specific comment by ID.
     *
     * @param commentId the comment ID
     * @return optional comment response
     */
    public Optional<CommentResponse> getComment(String commentId) {
        log.debug("Fetching comment: {}", commentId);
        try {
            CommentResponse response = executeGet(
                SERVICE_PATH_PREFIX + "/" + commentId,
                CommentResponse.class);
            return Optional.ofNullable(response);
        } catch (ServiceClientException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Updates an existing comment.
     *
     * @param commentId the comment ID
     * @param request the update request
     * @return the updated comment response
     */
    public CommentResponse updateComment(String commentId, UpdateCommentRequest request) {
        log.info("Updating comment: {}", commentId);
        try {
            return executePut(SERVICE_PATH_PREFIX + "/" + commentId, request, CommentResponse.class);
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(serviceName, SERVICE_PATH_PREFIX + "/" + commentId, e);
        }
    }

    /**
     * Deletes a comment.
     *
     * @param commentId the comment ID
     */
    public void deleteComment(String commentId) {
        log.info("Deleting comment: {}", commentId);
        executeDelete(SERVICE_PATH_PREFIX + "/" + commentId);
    }

    /**
     * Adds a comment with retry logic for resilience.
     *
     * @param issueId the issue ID
     * @param request the comment creation request
     * @param maxRetries maximum number of retry attempts
     * @return the created comment response
     */
    public CommentResponse addCommentWithRetry(String issueId, CreateCommentRequest request, int maxRetries) {
        return executeWithRetry(() -> addComment(issueId, request), maxRetries, 500);
    }

    /**
     * Batch add comments to an issue.
     *
     * @param issueId the issue ID
     * @param requests list of comment creation requests
     * @return list of created comment responses
     */
    public List<CommentResponse> addCommentsBatch(String issueId, List<CreateCommentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Adding {} comments to issue {} in batch", requests.size(), issueId);
        try {
            CommentResponse[] response = executePost(
                SERVICE_PATH_PREFIX + "/issue/" + issueId + "/batch",
                requests,
                CommentResponse[].class);

            if (response != null) {
                return Arrays.asList(response);
            }
            return Collections.emptyList();
        } catch (ServiceClientException e) {
            log.error("Batch comment creation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/issue/" + issueId + "/batch", e);
        }
    }

    /**
     * Gets comments for an issue with pagination.
     *
     * @param issueId the issue ID
     * @param page page number (0-based)
     * @param size page size
     * @return list of comment responses
     */
    public List<CommentResponse> getIssueCommentsPaginated(String issueId, int page, int size) {
        log.debug("Fetching comments for issue: {} (page={}, size={})", issueId, page, size);
        try {
            CommentResponse[] response = executeGet(
                SERVICE_PATH_PREFIX + "/issue/" + issueId + "?page=" + page + "&size=" + size,
                CommentResponse[].class);

            if (response != null) {
                return Arrays.asList(response);
            }
            return Collections.emptyList();
        } catch (ServiceClientException e) {
            if (e.getStatusCode() == 404) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    /**
     * Gets the count of comments for an issue.
     *
     * @param issueId the issue ID
     * @return the number of comments
     */
    public int getIssueCommentCount(String issueId) {
        log.debug("Getting comment count for issue: {}", issueId);
        List<CommentResponse> comments = getIssueComments(issueId);
        return comments.size();
    }
}