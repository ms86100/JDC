package com.jira.attachment.service;

import com.jira.attachment.dto.AttachmentResponse;
import com.jira.attachment.entity.Attachment;
import com.jira.attachment.exception.AttachmentNotFoundException;
import com.jira.attachment.exception.InvalidFileException;
import com.jira.attachment.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${jira.attachment.storage.path:/var/jira/attachments}")
    private String storagePath;

    private final List<String> allowedTypes = List.of(
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "application/pdf", "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip", "application/x-zip-compressed"
    );

    @Transactional
    public AttachmentResponse uploadAttachment(UUID issueId, MultipartFile file, UUID uploaderId, String uploaderName) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID() + "_" + sanitizeFilename(originalFilename);

        try {
            Path storageDir = Paths.get(storagePath, issueId.toString());
            Files.createDirectories(storageDir);
            Path filePath = storageDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = Attachment.builder()
                    .issueId(issueId)
                    .filename(storedFilename)
                    .originalFilename(originalFilename)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(filePath.toString())
                    .uploaderId(uploaderId)
                    .uploaderName(uploaderName)
                    .build();

            attachment = attachmentRepository.save(attachment);
            log.info("Uploaded attachment {} for issue {}", attachment.getId(), issueId);

            return toResponse(attachment);
        } catch (IOException e) {
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
            Path filePath = Paths.get(attachment.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new AttachmentNotFoundException("File not found on disk: " + attachmentId);
            }
        } catch (IOException e) {
            throw new AttachmentNotFoundException("Failed to read file: " + attachmentId, e);
        }
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found: " + attachmentId));

        try {
            Path filePath = Paths.get(attachment.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file from disk: {}", attachment.getStoragePath(), e);
        }

        attachmentRepository.delete(attachment);
        log.info("Deleted attachment {}", attachmentId);
    }

    @Transactional
    public void deleteAttachmentsByIssue(UUID issueId) {
        List<Attachment> attachments = attachmentRepository.findByIssueIdOrderByCreatedAtDesc(issueId);

        for (Attachment attachment : attachments) {
            try {
                Path filePath = Paths.get(attachment.getStoragePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Failed to delete file from disk: {}", attachment.getStoragePath(), e);
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
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}