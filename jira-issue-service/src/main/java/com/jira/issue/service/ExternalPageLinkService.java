package com.jira.issue.service;

import com.jira.issue.dto.ExternalPageLinkRequest;
import com.jira.issue.dto.ExternalPageLinkResponse;
import com.jira.issue.entity.ExternalPageLink;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.ExternalPageLinkRepository;
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
public class ExternalPageLinkService {

    private final ExternalPageLinkRepository externalPageLinkRepository;

    @Transactional
    public ExternalPageLinkResponse addPageLink(String entityType, UUID entityId,
                                                 ExternalPageLinkRequest request, UUID userId) {
        log.info("Adding page link for {} {} by user {}", entityType, entityId, userId);

        ExternalPageLink link = ExternalPageLink.builder()
                .entityType(entityType)
                .entityId(entityId)
                .url(request.getUrl())
                .title(request.getTitle())
                .applicationLinkId(request.getApplicationLinkId())
                .pageId(request.getPageId())
                .spaceKey(request.getSpaceKey())
                .linkedBy(userId)
                .build();

        link = externalPageLinkRepository.save(link);
        log.info("Page link added: {}", link.getId());

        return mapToResponse(link);
    }

    public List<ExternalPageLinkResponse> getPageLinks(String entityType, UUID entityId) {
        log.info("Getting page links for {} {}", entityType, entityId);
        return externalPageLinkRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removePageLink(UUID linkId) {
        log.info("Removing page link: {}", linkId);
        if (!externalPageLinkRepository.existsById(linkId)) {
            throw new ResourceNotFoundException("ExternalPageLink", "id", linkId);
        }
        externalPageLinkRepository.deleteById(linkId);
        log.info("Page link removed: {}", linkId);
    }

    private ExternalPageLinkResponse mapToResponse(ExternalPageLink link) {
        return ExternalPageLinkResponse.builder()
                .id(link.getId())
                .entityType(link.getEntityType())
                .entityId(link.getEntityId())
                .url(link.getUrl())
                .title(link.getTitle())
                .applicationLinkId(link.getApplicationLinkId())
                .pageId(link.getPageId())
                .spaceKey(link.getSpaceKey())
                .linkedBy(link.getLinkedBy())
                .linkedAt(link.getLinkedAt())
                .build();
    }
}
