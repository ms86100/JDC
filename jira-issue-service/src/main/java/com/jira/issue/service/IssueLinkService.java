package com.jira.issue.service;

import com.jira.issue.dto.*;
import com.jira.issue.entity.Issue;
import com.jira.issue.entity.IssueLink;
import com.jira.issue.entity.IssueLinkType;
import com.jira.issue.entity.IssueStatus;
import com.jira.issue.exception.DuplicateResourceException;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueLinkRepository;
import com.jira.issue.repository.IssueLinkTypeRepository;
import com.jira.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issue Link Service - Manages issue linking and link types (blocks, depends on, relates to, etc.)
 * Follows Jira DC patterns for issue linking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueLinkService {

    private final IssueLinkRepository issueLinkRepository;
    private final IssueLinkTypeRepository issueLinkTypeRepository;
    private final IssueRepository issueRepository;

    // ========== Link Type Management ==========

    /**
     * Create a new link type.
     */
    @Transactional
    public IssueLinkTypeResponse createLinkType(CreateLinkTypeRequest request) {
        log.info("Creating link type: {}", request.getName());

        if (issueLinkTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Link type", "name", request.getName());
        }

        IssueLinkType linkType = IssueLinkType.builder()
                .name(request.getName())
                .inward(request.getInward())
                .outward(request.getOutward())
                .isActive(true)
                .build();

        linkType = issueLinkTypeRepository.save(linkType);
        log.info("Created link type: {} ({})", linkType.getName(), linkType.getId());

        return toLinkTypeResponse(linkType);
    }

    /**
     * Get all link types.
     */
    @Transactional(readOnly = true)
    public List<IssueLinkTypeResponse> getAllLinkTypes() {
        return issueLinkTypeRepository.findAll().stream()
                .map(this::toLinkTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active link types only.
     */
    @Transactional(readOnly = true)
    public List<IssueLinkTypeResponse> getActiveLinkTypes() {
        return issueLinkTypeRepository.findByIsActiveTrue().stream()
                .map(this::toLinkTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get link type by ID.
     */
    @Transactional(readOnly = true)
    public IssueLinkTypeResponse getLinkTypeById(UUID linkTypeId) {
        IssueLinkType linkType = issueLinkTypeRepository.findById(linkTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("LinkType", "id", linkTypeId));
        return toLinkTypeResponse(linkType);
    }

    /**
     * Update a link type.
     */
    @Transactional
    public IssueLinkTypeResponse updateLinkType(UUID linkTypeId, UpdateLinkTypeRequest request) {
        log.info("Updating link type: {}", linkTypeId);

        IssueLinkType linkType = issueLinkTypeRepository.findById(linkTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("LinkType", "id", linkTypeId));

        if (request.getName() != null) linkType.setName(request.getName());
        if (request.getInward() != null) linkType.setInward(request.getInward());
        if (request.getOutward() != null) linkType.setOutward(request.getOutward());
        if (request.getIsActive() != null) linkType.setIsActive(request.getIsActive());

        linkType = issueLinkTypeRepository.save(linkType);
        log.info("Updated link type: {}", linkTypeId);

        return toLinkTypeResponse(linkType);
    }

    /**
     * Delete (deactivate) a link type.
     */
    @Transactional
    public void deleteLinkType(UUID linkTypeId) {
        log.info("Deleting/deactivating link type: {}", linkTypeId);

        IssueLinkType linkType = issueLinkTypeRepository.findById(linkTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("LinkType", "id", linkTypeId));

        // Check if link type is in use
        List<IssueLink> links = issueLinkRepository.findByLinkTypeId(linkTypeId);
        if (!links.isEmpty()) {
            // Soft delete - just deactivate
            linkType.setIsActive(false);
            issueLinkTypeRepository.save(linkType);
            log.info("Deactivated link type {} ({} links still reference it)", linkTypeId, links.size());
        } else {
            issueLinkTypeRepository.delete(linkType);
            log.info("Deleted link type: {}", linkTypeId);
        }
    }

    /**
     * Seed default link types if none exist.
     */
    @Transactional
    public void seedDefaultLinkTypes() {
        if (issueLinkTypeRepository.count() > 0) {
            log.debug("Link types already exist, skipping seed");
            return;
        }

        log.info("Seeding default link types");

        createDefaultLinkType("Blocks", "is blocked by", "blocks");
        createDefaultLinkType("Is blocked by", "blocks", "is blocked by");
        createDefaultLinkType("Duplicates", "is duplicated by", "duplicates");
        createDefaultLinkType("Is duplicated by", "duplicates", "is duplicated by");
        createDefaultLinkType("Relates to", "relates to", "relates to");
        createDefaultLinkType("Causes", "is caused by", "causes");
        createDefaultLinkType("Is caused by", "causes", "is caused by");
        createDefaultLinkType("Depends on", "is depended upon by", "depends on");
        createDefaultLinkType("Is depended upon by", "depends on", "is depended upon by");
        createDefaultLinkType("Clones", "is cloned by", "clones");
        createDefaultLinkType("Is cloned by", "clones", "is cloned by");
        createDefaultLinkType("Splits into", "is split from", "splits into");
        createDefaultLinkType("Is split from", "splits into", "is split from");
        createDefaultLinkType("Supercedes", "is superseded by", "supercedes");
        createDefaultLinkType("Is superseded by", "supercedes", "is superseded by");

        log.info("Seeded default link types");
    }

    private void createDefaultLinkType(String name, String inward, String outward) {
        if (!issueLinkTypeRepository.existsByNameIgnoreCase(name)) {
            IssueLinkType linkType = IssueLinkType.builder()
                    .name(name)
                    .inward(inward)
                    .outward(outward)
                    .isActive(true)
                    .build();
            issueLinkTypeRepository.save(linkType);
        }
    }

    // ========== Issue Links ==========

    /**
     * Create an issue link.
     */
    @Transactional
    public IssueLinkResponse createIssueLink(IssueLinkRequest request) {
        if (request.getLinkTypeId() == null) {
            request.setLinkTypeId(resolveLinkTypeId(request.getLinkTypeName()));
        }
        if (request.getLinkTypeId() == null) {
            throw new IllegalArgumentException("linkTypeId or linkTypeName is required");
        }

        if (!issueRepository.existsById(request.getSourceIssueId())) {
            throw new ResourceNotFoundException("Source issue not found: " + request.getSourceIssueId());
        }
        if (!issueRepository.existsById(request.getTargetIssueId())) {
            throw new ResourceNotFoundException("Target issue not found: " + request.getTargetIssueId());
        }

        if (issueLinkRepository.existsBySourceIssueIdAndTargetIssueIdAndLinkTypeId(
                request.getSourceIssueId(), request.getTargetIssueId(), request.getLinkTypeId())) {
            throw new DuplicateResourceException("Issue link", "source+target+type",
                    request.getSourceIssueId() + "+" + request.getTargetIssueId() + "+" + request.getLinkTypeId());
        }

        IssueLink issueLink = IssueLink.builder()
                .sourceIssueId(request.getSourceIssueId())
                .targetIssueId(request.getTargetIssueId())
                .linkTypeId(request.getLinkTypeId())
                .build();

        issueLink = issueLinkRepository.save(issueLink);
        log.info("Created issue link {} from {} to {} ({})",
                issueLink.getId(), request.getSourceIssueId(), request.getTargetIssueId(), request.getLinkTypeId());

        return toLinkResponse(issueLink);
    }

    /**
     * Get all links for an issue (both directions).
     */
    @Transactional(readOnly = true)
    public List<IssueLinkResponse> getLinksByIssue(UUID issueId) {
        List<IssueLink> links = issueLinkRepository.findBySourceIssueIdOrTargetIssueId(issueId, issueId);
        return links.stream()
                .map(this::toLinkResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get outward links only.
     */
    @Transactional(readOnly = true)
    public List<IssueLinkResponse> getOutwardLinks(UUID issueId) {
        return issueLinkRepository.findBySourceIssueId(issueId)
                .stream()
                .map(this::toLinkResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get inward links only.
     */
    @Transactional(readOnly = true)
    public List<IssueLinkResponse> getInwardLinks(UUID issueId) {
        return issueLinkRepository.findByTargetIssueId(issueId)
                .stream()
                .map(this::toLinkResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get links for workflow context.
     */
    @Transactional(readOnly = true)
    public List<IssueLinkWorkflowContextResponse> getLinksForWorkflow(UUID issueId) {
        List<IssueLink> links = issueLinkRepository.findBySourceIssueIdOrTargetIssueId(issueId, issueId);
        List<IssueLinkWorkflowContextResponse> result = new java.util.ArrayList<>();
        for (IssueLink link : links) {
            boolean outward = issueId.equals(link.getSourceIssueId());
            UUID linkedIssueId = outward ? link.getTargetIssueId() : link.getSourceIssueId();
            Issue linkedIssue = issueRepository.findById(linkedIssueId).orElse(null);
            if (linkedIssue == null) {
                continue;
            }
            String linkTypeName = resolveLinkTypeName(link.getLinkTypeId());
            IssueStatus status = linkedIssue.getStatus();
            result.add(IssueLinkWorkflowContextResponse.builder()
                    .linkId(link.getId())
                    .linkType(linkTypeName)
                    .direction(outward ? "OUTWARD" : "INWARD")
                    .linkedIssueId(linkedIssueId)
                    .linkedIssueKey(linkedIssue.getIssueKey())
                    .statusId(status != null ? status.getId() : null)
                    .statusName(status != null ? status.getName() : null)
                    .build());
        }
        return result;
    }

    /**
     * Delete an issue link.
     */
    @Transactional
    public void deleteIssueLink(UUID linkId) {
        if (!issueLinkRepository.existsById(linkId)) {
            throw new ResourceNotFoundException("Issue link not found: " + linkId);
        }
        issueLinkRepository.deleteById(linkId);
        log.info("Deleted issue link {}", linkId);
    }

    /**
     * Delete all links for an issue.
     */
    @Transactional
    public int deleteLinksByIssue(UUID issueId) {
        List<IssueLink> links = issueLinkRepository.findBySourceIssueIdOrTargetIssueId(issueId, issueId);
        int count = links.size();
        issueLinkRepository.deleteAll(links);
        log.info("Deleted {} links for issue {}", count, issueId);
        return count;
    }

    /**
     * Get link by ID.
     */
    @Transactional(readOnly = true)
    public IssueLinkResponse getLinkById(UUID linkId) {
        IssueLink link = issueLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue link", "id", linkId));
        return toLinkResponse(link);
    }

    /**
     * Get available link type names.
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableLinkTypes() {
        return issueLinkTypeRepository.findByIsActiveTrue().stream()
                .map(IssueLinkType::getName)
                .collect(Collectors.toList());
    }

    // ========== Private Helpers ==========

    private IssueLinkResponse toLinkResponse(IssueLink link) {
        Issue sourceIssue = issueRepository.findById(link.getSourceIssueId()).orElse(null);
        Issue destIssue = issueRepository.findById(link.getTargetIssueId()).orElse(null);

        String linkTypeName = resolveLinkTypeName(link.getLinkTypeId());
        String linkTypeLabel = resolveLinkTypeLabel(link.getLinkTypeId(), link.getSourceIssueId());

        return IssueLinkResponse.builder()
                .id(link.getId())
                .sourceIssueId(link.getSourceIssueId())
                .sourceIssueKey(sourceIssue != null ? sourceIssue.getIssueKey() : null)
                .targetIssueId(link.getTargetIssueId())
                .targetIssueKey(destIssue != null ? destIssue.getIssueKey() : null)
                .linkType(linkTypeName)
                .linkTypeLabel(linkTypeLabel)
                .sequence(link.getSequence())
                .createdAt(link.getCreatedAt())
                .build();
    }

    private IssueLinkTypeResponse toLinkTypeResponse(IssueLinkType linkType) {
        return IssueLinkTypeResponse.builder()
                .id(linkType.getId())
                .name(linkType.getName())
                .inward(linkType.getInward())
                .outward(linkType.getOutward())
                .isActive(linkType.getIsActive())
                .createdAt(linkType.getCreatedAt())
                .build();
    }

    private String resolveLinkTypeName(UUID linkTypeId) {
        if (linkTypeId == null) {
            return "Related";
        }
        return issueLinkTypeRepository.findById(linkTypeId)
                .map(IssueLinkType::getName)
                .orElse("Related");
    }

    private String resolveLinkTypeLabel(UUID linkTypeId, UUID sourceIssueId) {
        if (linkTypeId == null) {
            return "relates to";
        }
        return issueLinkTypeRepository.findById(linkTypeId)
                .map(lt -> {
                    // Determine direction based on link context (simplified)
                    return lt.getOutward();
                })
                .orElse("relates to");
    }

    private UUID resolveLinkTypeId(String linkTypeName) {
        if (linkTypeName == null || linkTypeName.isBlank()) {
            return null;
        }
        String normalized = linkTypeName.trim().toLowerCase();
        return issueLinkTypeRepository.findByNameIgnoreCase(normalized)
                .or(() -> issueLinkTypeRepository.findByNameIgnoreCase(normalized.replace('_', ' ')))
                .map(IssueLinkType::getId)
                .orElse(null);
    }
}