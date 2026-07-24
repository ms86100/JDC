package com.jira.document.service;

import com.jira.document.dto.*;
import com.jira.document.entity.*;
import com.jira.document.exception.ResourceNotFoundException;
import com.jira.document.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final LegalArchiveRepository legalArchiveRepository;
    private final LegalHoldRepository legalHoldRepository;

    // Document Management
    @Transactional
    public DocumentResponse createDocument(CreateDocumentRequest request, UUID userId) {
        log.info("Creating document '{}' for user {}", request.getTitle(), userId);

        Document document = Document.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .projectId(request.getProjectId())
                .issueId(request.getIssueId())
                .ownerId(userId)
                .documentType(request.getDocumentType())
                .space(request.getSpace())
                .parentDocumentId(request.getParentDocumentId())
                .versionLabel(request.getVersionLabel())
                .attachmentUrl(request.getAttachmentUrl())
                .metadata(request.getMetadata())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .pageLayout(request.getPageLayout() != null ? request.getPageLayout() : "DEFAULT")
                .labels(request.getLabels())
                .externalUrl(request.getExternalUrl())
                .build();

        document = documentRepository.save(document);

        // Create initial version
        createVersion(document.getId(), request.getContent(), "Initial version", userId);

        return toDocumentResponse(document);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        return toDocumentResponse(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentsByOwner(UUID ownerId, Pageable pageable) {
        return documentRepository.findByOwnerId(ownerId, pageable)
                .map(this::toDocumentResponse);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByProject(UUID projectId) {
        return documentRepository.findActiveByProjectId(projectId).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentResponse updateDocument(UUID documentId, CreateDocumentRequest request, UUID userId) {
        log.info("Updating document {}", documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setDocumentType(request.getDocumentType());
        document.setMetadata(request.getMetadata());

        document = documentRepository.save(document);

        return toDocumentResponse(document);
    }

    @Transactional
    public void archiveDocument(UUID documentId) {
        log.info("Archiving document: {}", documentId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        document.setIsArchived(true);
        documentRepository.save(document);
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        log.info("Deleting document: {}", documentId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        documentRepository.delete(document);
    }

    // Version Management
    @Transactional
    public DocumentVersionResponse createVersion(UUID documentId, String content, String changeSummary, UUID userId) {
        Integer maxVersion = documentVersionRepository.findMaxVersionNumberByDocumentId(documentId).orElse(0);

        DocumentVersion version = DocumentVersion.builder()
                .documentId(documentId)
                .versionNumber(maxVersion + 1)
                .content(content)
                .changeSummary(changeSummary)
                .createdBy(userId)
                .build();

        version = documentVersionRepository.save(version);

        return toDocumentVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> getDocumentVersions(UUID documentId) {
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(this::toDocumentVersionResponse)
                .collect(Collectors.toList());
    }

    // Legal Archive
    @Transactional
    public LegalArchiveResponse createLegalArchive(CreateLegalArchiveRequest request, UUID userId) {
        log.info("Creating legal archive '{}' by user {}", request.getName(), userId);

        LegalArchive archive = LegalArchive.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .legalMatterId(request.getLegalMatterId())
                .matterReference(request.getMatterReference())
                .archiveType(request.getArchiveType())
                .status("ACTIVE")
                .retentionDate(request.getRetentionDate())
                .dispositionAction(request.getDispositionAction())
                .legalBasis(request.getLegalBasis())
                .reason(request.getReason())
                .archivedBy(userId)
                .relatedDocumentIds(request.getRelatedDocumentIds())
                .relatedIssueIds(request.getRelatedIssueIds())
                .metadata(request.getMetadata())
                .build();

        archive = legalArchiveRepository.save(archive);

        return toLegalArchiveResponse(archive);
    }

    @Transactional(readOnly = true)
    public LegalArchiveResponse getLegalArchive(UUID archiveId) {
        LegalArchive archive = legalArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("LegalArchive", "id", archiveId));
        return toLegalArchiveResponse(archive);
    }

    @Transactional(readOnly = true)
    public List<LegalArchiveResponse> getLegalArchivesByProject(UUID projectId) {
        return legalArchiveRepository.findByProjectId(projectId).stream()
                .map(this::toLegalArchiveResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LegalArchiveResponse updateLegalArchiveStatus(UUID archiveId, String status) {
        log.info("Updating legal archive {} status to {}", archiveId, status);
        LegalArchive archive = legalArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("LegalArchive", "id", archiveId));
        archive.setStatus(status);
        archive = legalArchiveRepository.save(archive);
        return toLegalArchiveResponse(archive);
    }

    // Legal Hold
    @Transactional
    public LegalHoldResponse createLegalHold(CreateLegalHoldRequest request, UUID userId) {
        log.info("Creating legal hold '{}' by user {}", request.getName(), userId);

        LegalHold hold = LegalHold.builder()
                .name(request.getName())
                .description(request.getDescription())
                .legalMatterId(request.getLegalMatterId())
                .matterReference(request.getMatterReference())
                .holdType(request.getHoldType())
                .status("PENDING")
                .initiatedBy(userId)
                .custodianIds(request.getCustodianIds())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .autoExtend(request.getAutoExtend() != null ? request.getAutoExtend() : false)
                .extensionPeriodDays(request.getExtensionPeriodDays() != null ? request.getExtensionPeriodDays() : 30)
                .scope(request.getScope())
                .preservationInstructions(request.getPreservationInstructions())
                .dataCategories(request.getDataCategories())
                .projectIds(request.getProjectIds())
                .legalBasis(request.getLegalBasis())
                .isCritical(request.getIsCritical() != null ? request.getIsCritical() : false)
                .metadata(request.getMetadata())
                .build();

        hold = legalHoldRepository.save(hold);

        return toLegalHoldResponse(hold);
    }

    @Transactional
    public LegalHoldResponse activateLegalHold(UUID holdId) {
        log.info("Activating legal hold: {}", holdId);
        LegalHold hold = legalHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("LegalHold", "id", holdId));
        hold.setStatus("ACTIVE");
        hold = legalHoldRepository.save(hold);
        return toLegalHoldResponse(hold);
    }

    @Transactional
    public LegalHoldResponse releaseLegalHold(UUID holdId, UUID userId, String reason) {
        log.info("Releasing legal hold: {} by user {}", holdId, userId);
        LegalHold hold = legalHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("LegalHold", "id", holdId));
        hold.setStatus("RELEASED");
        hold.setReleasedAt(java.time.LocalDateTime.now());
        hold.setReleasedBy(userId);
        hold.setReleaseReason(reason);
        hold = legalHoldRepository.save(hold);
        return toLegalHoldResponse(hold);
    }

    @Transactional(readOnly = true)
    public LegalHoldResponse getLegalHold(UUID holdId) {
        LegalHold hold = legalHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("LegalHold", "id", holdId));
        return toLegalHoldResponse(hold);
    }

    @Transactional(readOnly = true)
    public List<LegalHoldResponse> getActiveLegalHolds() {
        return legalHoldRepository.findByStatus("ACTIVE").stream()
                .map(this::toLegalHoldResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LegalHoldResponse> getLegalHoldsByCustodian(UUID custodianId) {
        return legalHoldRepository.findByCustodianContaining(custodianId).stream()
                .map(this::toLegalHoldResponse)
                .collect(Collectors.toList());
    }

    // Response mappings
    private DocumentResponse toDocumentResponse(Document document) {
        Integer versionCount = documentVersionRepository.findMaxVersionNumberByDocumentId(document.getId()).orElse(0);

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .projectId(document.getProjectId())
                .issueId(document.getIssueId())
                .ownerId(document.getOwnerId())
                .documentType(document.getDocumentType())
                .space(document.getSpace())
                .parentDocumentId(document.getParentDocumentId())
                .versionLabel(document.getVersionLabel())
                .attachmentUrl(document.getAttachmentUrl())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .metadata(document.getMetadata())
                .isPublished(document.getIsPublished())
                .isArchived(document.getIsArchived())
                .pageLayout(document.getPageLayout())
                .labels(document.getLabels())
                .externalUrl(document.getExternalUrl())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .publishedAt(document.getPublishedAt())
                .childPosition(document.getChildPosition())
                .versionCount(versionCount)
                .build();
    }

    private DocumentVersionResponse toDocumentVersionResponse(DocumentVersion version) {
        return DocumentVersionResponse.builder()
                .id(version.getId())
                .documentId(version.getDocumentId())
                .versionNumber(version.getVersionNumber())
                .content(version.getContent())
                .changeSummary(version.getChangeSummary())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .contentHash(version.getContentHash())
                .build();
    }

    private LegalArchiveResponse toLegalArchiveResponse(LegalArchive archive) {
        return LegalArchiveResponse.builder()
                .id(archive.getId())
                .name(archive.getName())
                .description(archive.getDescription())
                .projectId(archive.getProjectId())
                .legalMatterId(archive.getLegalMatterId())
                .matterReference(archive.getMatterReference())
                .archiveType(archive.getArchiveType())
                .status(archive.getStatus())
                .retentionDate(archive.getRetentionDate())
                .dispositionDate(archive.getDispositionDate())
                .dispositionAction(archive.getDispositionAction())
                .legalBasis(archive.getLegalBasis())
                .reason(archive.getReason())
                .archivedBy(archive.getArchivedBy())
                .relatedDocumentIds(archive.getRelatedDocumentIds())
                .relatedIssueIds(archive.getRelatedIssueIds())
                .metadata(archive.getMetadata())
                .createdAt(archive.getCreatedAt())
                .completedAt(archive.getCompletedAt())
                .reviewDate(archive.getReviewDate())
                .build();
    }

    private LegalHoldResponse toLegalHoldResponse(LegalHold hold) {
        return LegalHoldResponse.builder()
                .id(hold.getId())
                .name(hold.getName())
                .description(hold.getDescription())
                .legalMatterId(hold.getLegalMatterId())
                .matterReference(hold.getMatterReference())
                .holdType(hold.getHoldType())
                .status(hold.getStatus())
                .initiatedBy(hold.getInitiatedBy())
                .custodianIds(hold.getCustodianIds())
                .custodianNames(hold.getCustodianNames())
                .startDate(hold.getStartDate())
                .endDate(hold.getEndDate())
                .autoExtend(hold.getAutoExtend())
                .extensionPeriodDays(hold.getExtensionPeriodDays())
                .scope(hold.getScope())
                .preservationInstructions(hold.getPreservationInstructions())
                .dataCategories(hold.getDataCategories())
                .projectIds(hold.getProjectIds())
                .legalBasis(hold.getLegalBasis())
                .isCritical(hold.getIsCritical())
                .notificationSent(hold.getNotificationSent())
                .lastNotificationAt(hold.getLastNotificationAt())
                .metadata(hold.getMetadata())
                .createdAt(hold.getCreatedAt())
                .updatedAt(hold.getUpdatedAt())
                .releasedAt(hold.getReleasedAt())
                .releasedBy(hold.getReleasedBy())
                .releaseReason(hold.getReleaseReason())
                .build();
    }
}