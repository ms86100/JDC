package com.jira.document.service;

import com.jira.document.dto.*;
import com.jira.document.entity.*;
import com.jira.document.exception.ResourceNotFoundException;
import com.jira.document.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private final MessageSource messageSource;

    @Value("${app.document.defaults.page-layout:DEFAULT}")
    private String defaultPageLayout;

    @Value("${app.document.defaults.is-published:false}")
    private boolean defaultIsPublished;

    @Value("${app.document.defaults.initial-version-label:Initial version}")
    private String initialVersionLabel;

    @Value("${app.document.defaults.content-updated-label:Content updated}")
    private String contentUpdatedLabel;

    @Value("${app.legal-archive.defaults.initial-status:ACTIVE}")
    private String archiveInitialStatus;

    @Value("${app.legal-archive.valid-statuses:ACTIVE,ARCHIVED,DISPOSED,DISPUTED}")
    private String validArchiveStatusesStr;

    @Value("${app.legal-hold.defaults.initial-status:PENDING}")
    private String holdInitialStatus;

    @Value("${app.legal-hold.defaults.active-status:ACTIVE}")
    private String holdActiveStatus;

    @Value("${app.legal-hold.defaults.released-status:RELEASED}")
    private String holdReleasedStatus;

    @Value("${app.legal-hold.defaults.auto-extend:false}")
    private boolean defaultAutoExtend;

    @Value("${app.legal-hold.defaults.extension-period-days:30}")
    private int defaultExtensionPeriodDays;

    @Value("${app.legal-hold.defaults.is-critical:false}")
    private boolean defaultIsCritical;

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
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : defaultIsPublished)
                .pageLayout(request.getPageLayout() != null ? request.getPageLayout() : defaultPageLayout)
                .labels(request.getLabels())
                .externalUrl(request.getExternalUrl())
                .build();

        document = documentRepository.save(document);

        // Create initial version
        createVersion(document.getId(), request.getContent(), initialVersionLabel, userId);

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

        // Before saving the updated document, create a version
        if (request.getContent() != null) {
            createVersion(documentId, request.getContent(), contentUpdatedLabel, userId);
        }

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
                .status(archiveInitialStatus)
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
        Set<String> validStatuses = Set.of(validArchiveStatusesStr.split(","));
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.archive.invalid-status",
                            new Object[]{status, validStatuses}, Locale.ENGLISH));
        }
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
                .status(holdInitialStatus)
                .initiatedBy(userId)
                .custodianIds(request.getCustodianIds())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .autoExtend(request.getAutoExtend() != null ? request.getAutoExtend() : defaultAutoExtend)
                .extensionPeriodDays(request.getExtensionPeriodDays() != null ? request.getExtensionPeriodDays() : defaultExtensionPeriodDays)
                .scope(request.getScope())
                .preservationInstructions(request.getPreservationInstructions())
                .dataCategories(request.getDataCategories())
                .projectIds(request.getProjectIds())
                .legalBasis(request.getLegalBasis())
                .isCritical(request.getIsCritical() != null ? request.getIsCritical() : defaultIsCritical)
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
        if (!holdInitialStatus.equals(hold.getStatus())) {
            throw new IllegalStateException(
                    messageSource.getMessage("error.hold.activate.invalid-status",
                            new Object[]{holdInitialStatus, hold.getStatus()}, Locale.ENGLISH));
        }
        hold.setStatus(holdActiveStatus);
        hold = legalHoldRepository.save(hold);
        return toLegalHoldResponse(hold);
    }

    @Transactional
    public LegalHoldResponse releaseLegalHold(UUID holdId, UUID userId, String reason) {
        log.info("Releasing legal hold: {} by user {}", holdId, userId);
        LegalHold hold = legalHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("LegalHold", "id", holdId));
        if (!holdActiveStatus.equals(hold.getStatus())) {
            throw new IllegalStateException(
                    messageSource.getMessage("error.hold.release.invalid-status",
                            new Object[]{holdActiveStatus, hold.getStatus()}, Locale.ENGLISH));
        }
        hold.setStatus(holdReleasedStatus);
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
        return legalHoldRepository.findByStatus(holdActiveStatus).stream()
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