package com.jira.migration.service.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.jira.migration.service.clients.dto.AttachmentResponse;
import java.util.*;

/**
 * Client for interacting with the Attachment Service.
 * Handles attachment upload, download, and metadata management.
 */
@Component
@Slf4j
public class AttachmentServiceClient extends BaseServiceClient {

    private static final String SERVICE_PATH_PREFIX = "/api/attachments";

    @Autowired
    public AttachmentServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            ServiceClientsConfig config) {
        super(restTemplate, objectMapper, circuitBreakerRegistry,
              "attachmentService", config.getAttachmentServiceUrl());
    }

    @Override
    protected String getCircuitBreakerName() {
        return "attachmentService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH_PREFIX;
    }

    /**
     * Upload an attachment to an issue.
     */
    public AttachmentResponse uploadAttachment(AttachmentUploadRequest request) {
        log.info("Uploading attachment {} to issue {} ({} bytes)",
                request.getFileName(), request.getIssueId(), request.getSize());
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("X-File-Name", request.getFileName());
            headers.set("X-Content-Type", request.getMimeType() != null ? request.getMimeType() : "application/octet-stream");

            org.springframework.http.HttpEntity<byte[]> entity =
                new org.springframework.http.HttpEntity<>(request.getContent(), headers);

            String url = buildUrl(SERVICE_PATH_PREFIX + "/issue/" + request.getIssueId());
            long startTime = System.currentTimeMillis();

            ResponseEntity<AttachmentResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                AttachmentResponse.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("POST {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);

            AttachmentResponse result = response.getBody();
            if (result != null && result.isSuccess()) {
                log.debug("Successfully uploaded attachment: {}", result.getId());
                return result;
            } else {
                throw ServiceClientException.serverError(serviceName, 500,
                    "Attachment upload failed: " + (result != null ? result.getErrorMessage() : "Unknown error"),
                    SERVICE_PATH_PREFIX, "POST");
            }
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/issue/" + request.getIssueId(), e);
        }
    }

    /**
     * Get all attachments for an issue.
     */
    public List<AttachmentResponse> getAttachmentsForIssue(String issueId) {
        log.debug("Fetching attachments for issue: {}", issueId);
        try {
            AttachmentResponse[] response = executeGet(
                SERVICE_PATH_PREFIX + "/issue/" + issueId,
                AttachmentResponse[].class);
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
     * Get attachment by ID.
     */
    public Optional<AttachmentResponse> getAttachmentById(String attachmentId) {
        log.debug("Fetching attachment by ID: {}", attachmentId);
        try {
            AttachmentResponse response = executeGet(
                SERVICE_PATH_PREFIX + "/" + attachmentId,
                AttachmentResponse.class);
            return Optional.ofNullable(response);
        } catch (ServiceClientException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Download attachment content.
     */
    public byte[] downloadAttachment(String attachmentId) {
        log.debug("Downloading attachment: {}", attachmentId);
        try {
            HttpHeaders headers = createHeaders();
            org.springframework.http.HttpEntity<Void> entity =
                new org.springframework.http.HttpEntity<>(headers);

            String url = buildUrl(SERVICE_PATH_PREFIX + "/" + attachmentId + "/download");
            long startTime = System.currentTimeMillis();

            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("GET {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);

            return response.getBody();
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(
                serviceName, SERVICE_PATH_PREFIX + "/" + attachmentId + "/download", e);
        }
    }

    /**
     * Delete an attachment.
     */
    public void deleteAttachment(String attachmentId) {
        log.info("Deleting attachment: {}", attachmentId);
        executeDelete(SERVICE_PATH_PREFIX + "/" + attachmentId);
    }

    /**
     * Update attachment metadata.
     */
    public AttachmentResponse updateAttachment(String attachmentId, AttachmentUpdateRequest request) {
        log.info("Updating attachment: {}", attachmentId);
        try {
            return executePut(SERVICE_PATH_PREFIX + "/" + attachmentId, request, AttachmentResponse.class);
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(serviceName, SERVICE_PATH_PREFIX + "/" + attachmentId, e);
        }
    }

    /**
     * Request DTO for attachment upload.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AttachmentUploadRequest {
        private String issueId;
        private String fileName;
        private byte[] content;
        private long size;
        private String mimeType;
        private String uploadedBy;
        private Map<String, Object> properties;
    }

    /**
     * Request DTO for attachment update.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AttachmentUpdateRequest {
        private String fileName;
        private String description;
        private Map<String, Object> properties;
    }
}