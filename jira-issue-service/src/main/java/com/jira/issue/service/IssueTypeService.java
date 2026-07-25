package com.jira.issue.service;

import com.jira.issue.config.CacheConfig;
import com.jira.issue.dto.IssueTypeRequest;
import com.jira.issue.dto.IssueTypeResponse;
import com.jira.issue.entity.IssueType;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Issue Types with caching support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueTypeService {

    private final IssueTypeRepository issueTypeRepository;
    private final com.jira.issue.repository.IssueRepository issueRepository;

    @Value("${app.defaults.issue-type-icon:standard}")
    private String defaultIssueTypeIcon;

    @Cacheable(value = CacheConfig.ISSUE_TYPE_CACHE, key = "'all'")
    @Transactional(readOnly = true)
    public List<IssueTypeResponse> getAllIssueTypes() {
        log.debug("Fetching all issue types from database (cache miss)");
        return issueTypeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.ISSUE_TYPE_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public IssueTypeResponse getIssueType(UUID id) {
        log.debug("Fetching issue type from database (cache miss): {}", id);
        IssueType issueType = issueTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IssueType", "id", id));
        return mapToResponse(issueType);
    }

    @Cacheable(value = CacheConfig.ISSUE_TYPE_CACHE, key = "'name:' + #request.name")
    @Transactional(readOnly = true)
    public IssueType findByNameCached(String name) {
        return issueTypeRepository.findByName(name).orElse(null);
    }

    @CacheEvict(value = CacheConfig.ISSUE_TYPE_CACHE, allEntries = true)
    @Transactional
    public IssueTypeResponse createIssueType(IssueTypeRequest request) {
        log.info("Creating issue type: {}", request.getName());

        if (issueTypeRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Issue type with name '" + request.getName() + "' already exists");
        }

        if (request.getIssueTypeKey() != null && issueTypeRepository.findByIssueTypeKey(request.getIssueTypeKey()).isPresent()) {
            throw new IllegalArgumentException("Issue type with key '" + request.getIssueTypeKey() + "' already exists");
        }

        String issueTypeKey = request.getIssueTypeKey() != null
            ? request.getIssueTypeKey()
            : request.getName().toLowerCase().replace(" ", "-");

        IssueType issueType = IssueType.builder()
                .name(request.getName())
                .issueTypeKey(issueTypeKey)
                .description(request.getDescription())
                .icon(request.getIcon() != null ? request.getIcon() : defaultIssueTypeIcon)
                .color(request.getColor())
                .isSubtask(request.isSubtask())
                .sequence(request.getSequence())
                .build();

        issueType = issueTypeRepository.save(issueType);
        log.info("Created issue type: {}", issueType.getId());

        return mapToResponse(issueType);
    }

    @CacheEvict(value = CacheConfig.ISSUE_TYPE_CACHE, allEntries = true)
    @Transactional
    public IssueTypeResponse updateIssueType(UUID id, IssueTypeRequest request) {
        log.info("Updating issue type: {}", id);

        IssueType issueType = issueTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IssueType", "id", id));

        if (request.getName() != null) {
            issueType.setName(request.getName());
        }
        if (request.getDescription() != null) {
            issueType.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            issueType.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            issueType.setColor(request.getColor());
        }
        issueType.setIsSubtask(request.isSubtask());
        if (request.getSequence() > 0) {
            issueType.setSequence(request.getSequence());
        }

        issueType = issueTypeRepository.save(issueType);
        log.info("Updated issue type: {}", id);

        return mapToResponse(issueType);
    }

    @CacheEvict(value = CacheConfig.ISSUE_TYPE_CACHE, allEntries = true)
    @Transactional
    public void deleteIssueType(UUID id) {
        log.info("Deleting issue type: {}", id);

        IssueType issueType = issueTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IssueType", "id", id));

        long issueCount = issueRepository.countByIssueTypeId(id);
        if (issueCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete issue type '" + issueType.getName() + "' because it is used by "
                            + issueCount + " issue(s). Migrate or reassign those issues first.");
        }

        issueTypeRepository.deleteById(id);
        log.info("Deleted issue type: {}", id);
    }

    @CacheEvict(value = CacheConfig.ISSUE_TYPE_CACHE, allEntries = true)
    public void clearCache() {
        log.info("Clearing issue type cache");
    }

    private IssueTypeResponse mapToResponse(IssueType issueType) {
        return IssueTypeResponse.builder()
                .id(issueType.getId())
                .name(issueType.getName())
                .description(issueType.getDescription())
                .issueTypeKey(issueType.getIssueTypeKey())
                .isSubtask(Boolean.TRUE.equals(issueType.getIsSubtask()))
                .icon(issueType.getIcon())
                .color(issueType.getColor())
                .sequence(issueType.getSequence() != null ? issueType.getSequence() : 0)
                .createdAt(issueType.getCreatedAt())
                .updatedAt(issueType.getUpdatedAt())
                .build();
    }
}