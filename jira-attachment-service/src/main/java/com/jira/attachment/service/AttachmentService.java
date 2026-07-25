package com.jira.attachment.service;

import com.jira.attachment.dto.AttachmentResponse;
import com.jira.attachment.entity.Attachment;
import com.jira.attachment.exception.AttachmentNotFoundException;
import com.jira.attachment.exception.InvalidFileException;
import com.jira.attachment.repository.AttachmentRepository;
import com.jira.cluster.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final StorageProvider storageProvider;

    @Value("${cdn.base-url:}")
    private String cdnBaseUrl;

    @Value("${jira.attachment.allowed-types:image/png,image/jpeg,image/gif,image/webp,application/pdf,text/plain,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/zip,application/x-zip-compressed}")
    private List<String> allowedTypes;

    @Transactional
    public AttachmentResponse uploadAttachment(UUID issueId, MultipartFile file, UUID uploaderId, String uploaderName) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID() + "_" + sanitizeFilename(originalFilename);
        String storageKey = issueId.toString() + "/" + storedFilename;

        try {
            storageProvider.store(storageKey, file.getInputStream(), file.getSize());

            Attachment attachment = Attachment.builder()
                    .issueId(issueId)
                    .filename(storedFilename)
                    .originalFilename(originalFilename)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(storageKey)
                    .uploaderId(uploaderId)
                    .uploaderName(uploaderName)
                    .build();

            attachment = attachmentRepository.save(attachment);
            log.info("Uploaded attachment {} for issue {}", attachment.getId(), issueId);

            return toResponse(attachment);
        } catch (IOException e) {
            log.error("Failed to store file for issue {}", issueId, e);
            throw new InvalidFileException("Failed to store file: " + e.getMessage(), e);
        } catch (UncheckedIOException e) {
            log.error("Failed to store file for issue {}", issueId, e);
            throw new InvalidFileException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByIssue(UUID issueId) {
        return attachmentRepository.findByIssueIdOrderByCreatedAtDesc(issueId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttachmentResponse getAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found: " + attachmentId));
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public Resource downloadAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found: " + attachmentId));

        try {
            InputStream stream = storageProvider.retrieve(attachment.getStoragePath());
            return new InputStreamResource(stream);
        } catch (UncheckedIOException e) {
            throw new AttachmentNotFoundException("File not found in storage: " + attachmentId, e);
        }
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found: " + attachmentId));

        try {
            storageProvider.delete(attachment.getStoragePath());
        } catch (UncheckedIOException e) {
            log.warn("Failed to delete file from storage: {}", attachment.getStoragePath(), e);
        }

        attachmentRepository.delete(attachment);
        log.info("Deleted attachment {}", attachmentId);
    }

    @Transactional
    public void deleteAttachmentsByIssue(UUID issueId) {
        List<Attachment> attachments = attachmentRepository.findByIssueIdOrderByCreatedAtDesc(issueId);

        for (Attachment attachment : attachments) {
            try {
                storageProvider.delete(attachment.getStoragePath());
            } catch (UncheckedIOException e) {
                log.warn("Failed to delete file from storage: {}", attachment.getStoragePath(), e);
            }
        }

        attachmentRepository.deleteByIssueId(issueId);
        log.info("Deleted all attachments for issue {}", issueId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new InvalidFileException("File type not allowed: " + contentType);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        String downloadUrl = (cdnBaseUrl != null && !cdnBaseUrl.isBlank())
                ? cdnBaseUrl + "/attachments/" + attachment.getId()
                : "/api/attachments/" + attachment.getId() + "/download";

        return AttachmentResponse.builder()
                .id(attachment.getId())
                .issueId(attachment.getIssueId())
                .filename(attachment.getOriginalFilename())
                .originalFilename(attachment.getOriginalFilename())
                .mimeType(attachment.getMimeType())
                .mimeTypeDetected(attachment.getMimeTypeDetected())
                .thumbnailPath(attachment.getThumbnailPath())
                .fileSize(attachment.getFileSize())
                .uploaderId(attachment.getUploaderId())
                .uploaderName(attachment.getUploaderName())
                .downloadUrl(downloadUrl)
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
