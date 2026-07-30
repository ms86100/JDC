package com.avionics_systems.attachment.service;

import com.avionics_systems.attachment.dto.AttachmentResponse;
import com.avionics_systems.attachment.entity.Attachment;
import com.avionics_systems.attachment.exception.AttachmentNotFoundException;
import com.avionics_systems.attachment.exception.InvalidFileException;
import com.avionics_systems.attachment.repository.AttachmentRepository;
import com.avionics_systems.cluster.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final StorageProvider storageProvider;
    private final MessageSource messageSource;

    @Value("${cdn.base-url:}")
    private String cdnBaseUrl;

    @Value("${avionics-systems.attachment.allowed-types:image/png,image/jpeg,image/gif,image/webp,application/pdf,text/plain,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/zip,application/x-zip-compressed}")
    private List<String> allowedTypes;

    @Value("${app.attachment.default-filename:unnamed}")
    private String defaultFilename;

    @Value("${app.attachment.cdn-path-prefix:/attachments/}")
    private String cdnPathPrefix;

    @Value("${app.attachment.api-download-pattern:/api/attachments/%s/download}")
    private String apiDownloadPattern;

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
            throw new InvalidFileException(messageSource.getMessage("error.file.store.failed",
                    new Object[]{e.getMessage()}, "Failed to store file: " + e.getMessage(), Locale.ENGLISH), e);
        } catch (UncheckedIOException e) {
            log.error("Failed to store file for issue {}", issueId, e);
            throw new InvalidFileException(messageSource.getMessage("error.file.store.failed",
                    new Object[]{e.getMessage()}, "Failed to store file: " + e.getMessage(), Locale.ENGLISH), e);
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
                .orElseThrow(() -> new AttachmentNotFoundException(messageSource.getMessage("error.attachment.not.found",
                        new Object[]{attachmentId}, "Attachment not found: " + attachmentId, Locale.ENGLISH)));
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public Resource downloadAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(messageSource.getMessage("error.attachment.not.found",
                        new Object[]{attachmentId}, "Attachment not found: " + attachmentId, Locale.ENGLISH)));

        try {
            InputStream stream = storageProvider.retrieve(attachment.getStoragePath());
            return new InputStreamResource(stream);
        } catch (UncheckedIOException e) {
            throw new AttachmentNotFoundException(messageSource.getMessage("error.file.not.found.storage",
                    new Object[]{attachmentId}, "File not found in storage: " + attachmentId, Locale.ENGLISH), e);
        }
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(messageSource.getMessage("error.attachment.not.found",
                        new Object[]{attachmentId}, "Attachment not found: " + attachmentId, Locale.ENGLISH)));

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
            throw new InvalidFileException(messageSource.getMessage("error.file.empty",
                    null, "File is empty", Locale.ENGLISH));
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new InvalidFileException(messageSource.getMessage("error.file.type.not.allowed",
                    new Object[]{contentType}, "File type not allowed: " + contentType, Locale.ENGLISH));
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return defaultFilename;
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        String downloadUrl = (cdnBaseUrl != null && !cdnBaseUrl.isBlank())
                ? cdnBaseUrl + cdnPathPrefix + attachment.getId()
                : String.format(apiDownloadPattern, attachment.getId());

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
