package com.jira.issue.service;

import com.jira.issue.dto.IssueTypeRequest;
import com.jira.issue.dto.IssueTypeResponse;
import com.jira.issue.entity.IssueType;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueTypeRepository;
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
public class IssueTypeService {

    private final IssueTypeRepository issueTypeRepository;

    @Transactional(readOnly = true)
    public List<IssueTypeResponse> getAllIssueTypes() {
        return issueTypeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IssueTypeResponse getIssueType(UUID id) {
        IssueType issueType = issueTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IssueType", "id", id));
        return mapToResponse(issueType);
    }

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
                .icon(request.getIcon() != null ? request.getIcon() : "standard")
                .color(request.getColor())
                .isSubtask(request.isSubtask())
                .sequence(request.getSequence())
                .build();

        issueType = issueTypeRepository.save(issueType);
        log.info("Created issue type: {}", issueType.getId());

        return mapToResponse(issueType);
    }

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

        issueType = issueTypeRepository.save(issueType);
        log.info("Updated issue type: {}", id);

        return mapToResponse(issueType);
    }

    @Transactional
    public void deleteIssueType(UUID id) {
        log.info("Deleting issue type: {}", id);

        if (!issueTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("IssueType", "id", id);
        }

        issueTypeRepository.deleteById(id);
        log.info("Deleted issue type: {}", id);
    }

    private IssueTypeResponse mapToResponse(IssueType issueType) {
        return IssueTypeResponse.builder()
                .id(issueType.getId())
                .name(issueType.getName())
                .description(issueType.getDescription())
                .issueTypeKey(issueType.getName().toLowerCase().replace(" ", "-"))
                .isSubtask(false)
                .icon(issueType.getIcon())
                .createdAt(issueType.getCreatedAt())
                .build();
    }
}