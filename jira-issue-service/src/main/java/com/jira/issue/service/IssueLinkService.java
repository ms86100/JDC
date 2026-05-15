package com.jira.issue.service;

import com.jira.issue.dto.IssueLinkRequest;
import com.jira.issue.dto.IssueLinkResponse;
import com.jira.issue.entity.Issue;
import com.jira.issue.entity.IssueLink;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueLinkRepository;
import com.jira.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueLinkService {

    private final IssueLinkRepository issueLinkRepository;
    private final IssueRepository issueRepository;

    private static final Map<String, String> LINK_TYPE_LABELS = Map.of(
            "blocks", "Blocks",
            "relates to", "Relates to",
            "duplicates", "Duplicates",
            "is cloned by", "Clones",
            "is parent of", "Parent",
            "causes", "Causes"
    );

    @Transactional
    public IssueLinkResponse createIssueLink(IssueLinkRequest request) {
        if (!issueRepository.existsById(request.getSourceIssueId())) {
            throw new ResourceNotFoundException("Source issue not found: " + request.getSourceIssueId());
        }
        if (!issueRepository.existsById(request.getDestinationIssueId())) {
            throw new ResourceNotFoundException("Destination issue not found: " + request.getDestinationIssueId());
        }

        if (issueLinkRepository.existsBySourceIssueIdAndDestinationIssueIdAndLinkType(
                request.getSourceIssueId(), request.getDestinationIssueId(), request.getLinkType())) {
            throw new IllegalArgumentException("Issue link already exists");
        }

        IssueLink issueLink = IssueLink.builder()
                .sourceIssueId(request.getSourceIssueId())
                .destinationIssueId(request.getDestinationIssueId())
                .linkType(request.getLinkType())
                .build();

        issueLink = issueLinkRepository.save(issueLink);
        log.info("Created issue link {} from {} to {} ({})",
                issueLink.getId(), request.getSourceIssueId(), request.getDestinationIssueId(), request.getLinkType());

        return toResponse(issueLink);
    }

    @Transactional(readOnly = true)
    public List<IssueLinkResponse> getLinksByIssue(UUID issueId) {
        List<IssueLink> links = issueLinkRepository.findBySourceIssueIdOrDestinationIssueId(issueId, issueId);
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
        return issueLinkRepository.findByDestinationIssueId(issueId)
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
        return Arrays.asList("blocks", "relates to", "duplicates", "is cloned by", "is parent of", "causes");
    }

    private IssueLinkResponse toResponse(IssueLink link) {
        Issue sourceIssue = issueRepository.findById(link.getSourceIssueId()).orElse(null);
        Issue destIssue = issueRepository.findById(link.getDestinationIssueId()).orElse(null);

        String label = LINK_TYPE_LABELS.getOrDefault(link.getLinkType(), link.getLinkType());

        return IssueLinkResponse.builder()
                .id(link.getId())
                .sourceIssueId(link.getSourceIssueId())
                .sourceIssueKey(sourceIssue != null ? sourceIssue.getIssueKey() : null)
                .destinationIssueId(link.getDestinationIssueId())
                .destinationIssueKey(destIssue != null ? destIssue.getIssueKey() : null)
                .linkType(link.getLinkType())
                .linkTypeLabel(label)
                .sequence(link.getSequence())
                .createdAt(link.getCreatedAt())
                .build();
    }
}