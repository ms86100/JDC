package com.jira.issue.service;

import com.jira.issue.dto.IssueLinkRequest;
import com.jira.issue.dto.IssueLinkResponse;
import com.jira.issue.dto.IssueLinkWorkflowContextResponse;
import com.jira.issue.entity.Issue;
import com.jira.issue.entity.IssueLink;
import com.jira.issue.entity.IssueLinkType;
import com.jira.issue.entity.IssueStatus;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueLinkService {

    private final IssueLinkRepository issueLinkRepository;
    private final IssueLinkTypeRepository issueLinkTypeRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public IssueLinkResponse createIssueLink(IssueLinkRequest request) {
        if (request.getLinkTypeId() == null) {
            request.setLinkTypeId(resolveLinkTypeId(request.getLinkTypeName()));
        }
        if (request.getLinkTypeId() == null) {
            throw new IllegalArgumentException("linkTypeId or linkType/linkTypeName is required");
        }

        if (!issueRepository.existsById(request.getSourceIssueId())) {
            throw new ResourceNotFoundException("Source issue not found: " + request.getSourceIssueId());
        }
        if (!issueRepository.existsById(request.getTargetIssueId())) {
            throw new ResourceNotFoundException("Target issue not found: " + request.getTargetIssueId());
        }

        if (issueLinkRepository.existsBySourceIssueIdAndTargetIssueIdAndLinkTypeId(
                request.getSourceIssueId(), request.getTargetIssueId(), request.getLinkTypeId())) {
            throw new IllegalArgumentException("Issue link already exists");
        }

        IssueLink issueLink = IssueLink.builder()
                .sourceIssueId(request.getSourceIssueId())
                .targetIssueId(request.getTargetIssueId())
                .linkTypeId(request.getLinkTypeId())
                .build();

        issueLink = issueLinkRepository.save(issueLink);
        log.info("Created issue link {} from {} to {} ({})",
                issueLink.getId(), request.getSourceIssueId(), request.getTargetIssueId(), request.getLinkTypeId());

        return toResponse(issueLink);
    }

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

    @Transactional(readOnly = true)
    public List<IssueLinkResponse> getLinksByIssue(UUID issueId) {
        List<IssueLink> links = issueLinkRepository.findBySourceIssueIdOrTargetIssueId(issueId, issueId);
        return links.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueLinkResponse> getOutwardLinks(UUID issueId) {
        return issueLinkRepository.findBySourceIssueId(issueId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueLinkResponse> getInwardLinks(UUID issueId) {
        return issueLinkRepository.findByTargetIssueId(issueId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteIssueLink(UUID linkId) {
        if (!issueLinkRepository.existsById(linkId)) {
            throw new ResourceNotFoundException("Issue link not found: " + linkId);
        }
        issueLinkRepository.deleteById(linkId);
        log.info("Deleted issue link {}", linkId);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableLinkTypes() {
        return issueLinkTypeRepository.findAll().stream()
                .map(IssueLinkType::getName)
                .collect(Collectors.toList());
    }

    private IssueLinkResponse toResponse(IssueLink link) {
        Issue sourceIssue = issueRepository.findById(link.getSourceIssueId()).orElse(null);
        Issue destIssue = issueRepository.findById(link.getTargetIssueId()).orElse(null);

        String linkTypeName = resolveLinkTypeName(link.getLinkTypeId());

        return IssueLinkResponse.builder()
                .id(link.getId())
                .sourceIssueId(link.getSourceIssueId())
                .sourceIssueKey(sourceIssue != null ? sourceIssue.getIssueKey() : null)
                .targetIssueId(link.getTargetIssueId())
                .targetIssueKey(destIssue != null ? destIssue.getIssueKey() : null)
                .linkType(linkTypeName)
                .linkTypeLabel(linkTypeName)
                .sequence(link.getSequence())
                .createdAt(link.getCreatedAt())
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