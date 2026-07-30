package com.avionics_systems.migration.service.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.avionics_systems.migration.service.clients.dto.AttachmentResponse;
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
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(request.getContent()) {
                @Override
                public String getFilename() {
                    return request.getFileName();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("issueId", request.getIssueId());
            if (request.getUploadedBy() != null) {
                body.add("uploaderId", request.getUploadedBy());
            }

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = buildUrl(SERVICE_PATH_PREFIX);
            long startTime = System.currentTimeMillis();

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("POST {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);

            Map<String, Object> remote = response.getBody();
            if (remote != null && remote.get("id") != null) {
                return AttachmentResponse.builder()
                        .id(String.valueOf(remote.get("id")))
                        .issueId(remote.get("issueId") != null
                                ? String.valueOf(remote.get("issueId")) : request.getIssueId())
                        .filename(remote.get("filename") != null
                                ? String.valueOf(remote.get("filename")) : request.getFileName())
                        .mimeType(remote.get("mimeType") != null ? String.valueOf(remote.get("mimeType")) : request.getMimeType())
                        .size(remote.get("fileSize") instanceof Number n ? n.longValue() : request.getSize())
                        .success(true)
                        .build();
            }
            throw ServiceClientException.serverError(serviceName, 500,
                    "Attachment upload failed: empty response",
                    SERVICE_PATH_PREFIX, "POST");
        } catch (ServiceClientException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceClientException.connectionError(serviceName, SERVICE_PATH_PREFIX, e);
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
     * Initialize a chunked upload session. Falls back to synthetic session id when API unavailable.
     */
    public String initChunkedUpload(String issueId, String fileName, long totalSize, String checksum,
                                    String mimeType, String uploadedBy) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("issueId", issueId);
            body.put("fileName", fileName);
            body.put("totalSize", totalSize);
            body.put("checksum", checksum);
            body.put("mimeType", mimeType);
            if (uploadedBy != null) {
                body.put("uploadedBy", uploadedBy);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> response = executePost(
                    SERVICE_PATH_PREFIX + "/chunked/init", body, Map.class);
            if (response != null && response.get("sessionId") != null) {
                return String.valueOf(response.get("sessionId"));
            }
        } catch (ServiceClientException e) {
            log.debug("Chunked init not available, using local session: {}", e.getMessage());
        }
        String localId = "local-" + UUID.randomUUID();
        ChunkedUploadBuffer buffer = new ChunkedUploadBuffer();
        buffer.setMetadata(issueId, fileName, mimeType, uploadedBy);
        LOCAL_CHUNK_BUFFERS.put(localId, buffer);
        return localId;
    }

    public void prepareLocalChunkSession(String sessionId, String issueId, String fileName,
                                         String mimeType, String uploadedBy) {
        ChunkedUploadBuffer buffer = LOCAL_CHUNK_BUFFERS.get(sessionId);
        if (buffer != null) {
            buffer.setMetadata(issueId, fileName, mimeType, uploadedBy);
        }
    }

    /**
     * Upload one chunk. Local sessions buffer in-memory until complete.
     */
    public void uploadChunk(String sessionId, int chunkIndex, byte[] chunkData) {
        if (sessionId.startsWith("local-")) {
            ChunkedUploadBuffer buffer = LOCAL_CHUNK_BUFFERS.computeIfAbsent(sessionId, k -> new ChunkedUploadBuffer());
            buffer.addChunk(chunkIndex, chunkData);
            return;
        }
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<byte[]> entity = new HttpEntity<>(chunkData, headers);
            String url = buildUrl(SERVICE_PATH_PREFIX + "/chunked/" + sessionId + "/parts/" + chunkIndex);
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        } catch (ServiceClientException e) {
            if (e.getStatusCode() == 404) {
                ChunkedUploadBuffer buffer = LOCAL_CHUNK_BUFFERS.computeIfAbsent(sessionId, k -> new ChunkedUploadBuffer());
                buffer.addChunk(chunkIndex, chunkData);
            } else {
                throw e;
            }
        }
    }

    /**
     * Complete chunked upload and verify checksum.
     */
    public AttachmentResponse completeChunkedUpload(String sessionId, String checksum) {
        if (sessionId.startsWith("local-")) {
            ChunkedUploadBuffer buffer = LOCAL_CHUNK_BUFFERS.remove(sessionId);
            if (buffer == null) {
                throw ServiceClientException.serverError(serviceName, 500,
                        "Missing chunked buffer for session " + sessionId, SERVICE_PATH_PREFIX, "POST");
            }
            byte[] assembled = buffer.assemble();
            String actual = ChunkedUploadBuffer.sha256Hex(assembled);
            if (!actual.equalsIgnoreCase(checksum)) {
                throw new IllegalArgumentException("Chunked upload checksum mismatch");
            }
            AttachmentUploadRequest request = buffer.toUploadRequest(assembled);
            return uploadAttachment(request);
        }
        try {
            Map<String, Object> body = Map.of("checksum", checksum);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = executePost(
                    SERVICE_PATH_PREFIX + "/chunked/" + sessionId + "/complete", body, Map.class);
            if (response != null && response.get("id") != null) {
                return AttachmentResponse.builder()
                        .id(String.valueOf(response.get("id")))
                        .checksum(checksum)
                        .success(true)
                        .build();
            }
        } catch (ServiceClientException e) {
            if (e.getStatusCode() != 404) {
                throw e;
            }
        }
        throw ServiceClientException.serverError(serviceName, 500,
                "Chunked complete failed for session " + sessionId, SERVICE_PATH_PREFIX, "POST");
    }

    private static final Map<String, ChunkedUploadBuffer> LOCAL_CHUNK_BUFFERS = new java.util.concurrent.ConcurrentHashMap<>();

    static class ChunkedUploadBuffer {
        private final java.util.TreeMap<Integer, byte[]> chunks = new java.util.TreeMap<>();
        private String issueId;
        private String fileName;
        private String mimeType;
        private String uploadedBy;

        void addChunk(int index, byte[] data) {
            chunks.put(index, data);
        }

        byte[] assemble() {
            int total = chunks.values().stream().mapToInt(b -> b.length).sum();
            byte[] out = new byte[total];
            int offset = 0;
            for (byte[] part : chunks.values()) {
                System.arraycopy(part, 0, out, offset, part.length);
                offset += part.length;
            }
            return out;
        }

        AttachmentUploadRequest toUploadRequest(byte[] content) {
            return AttachmentUploadRequest.builder()
                    .issueId(issueId)
                    .fileName(fileName != null ? fileName : "upload.bin")
                    .content(content)
                    .size(content.length)
                    .mimeType(mimeType)
                    .uploadedBy(uploadedBy)
                    .build();
        }

        void setMetadata(String issueId, String fileName, String mimeType, String uploadedBy) {
            this.issueId = issueId;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.uploadedBy = uploadedBy;
        }

        static String sha256Hex(byte[] data) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                return java.util.HexFormat.of().formatHex(digest.digest(data));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
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