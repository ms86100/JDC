package com.jira.issue.service;

import com.jira.issue.dto.LabelRequest;
import com.jira.issue.dto.LabelResponse;
import com.jira.issue.entity.Label;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.IssueRepository;
import com.jira.issue.repository.LabelRepository;
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
public class LabelService {

    private final LabelRepository labelRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public LabelResponse addLabel(LabelRequest request) {
        if (!issueRepository.existsById(request.getIssueId())) {
            throw new ResourceNotFoundException("Issue not found: " + request.getIssueId());
        }

        if (labelRepository.existsByIssueIdAndNameIgnoreCase(request.getIssueId(), request.getName())) {
            throw new IllegalArgumentException("Label already exists: " + request.getName());
        }

        Label label = Label.builder()
                .issueId(request.getIssueId())
                .name(request.getName().toLowerCase().trim())
                .build();

        label = labelRepository.save(label);
        log.info("Added label {} to issue {}", label.getName(), request.getIssueId());

        return toResponse(label);
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> getLabelsByIssue(UUID issueId) {
        return labelRepository.findByIssueId(issueId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> searchLabels(String query) {
        return labelRepository.findByNameIgnoreCase("%" + query.toLowerCase() + "%")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeLabel(UUID issueId, String labelName) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue not found: " + issueId);
        }
        labelRepository.deleteByIssueIdAndNameIgnoreCase(issueId, labelName);
        log.info("Removed label {} from issue {}", labelName, issueId);
    }

    @Transactional
    public void removeLabelById(UUID labelId) {
        if (!labelRepository.existsById(labelId)) {
            throw new ResourceNotFoundException("Label not found: " + labelId);
        }
        labelRepository.deleteById(labelId);
        log.info("Deleted label {}", labelId);
    }

    private LabelResponse toResponse(Label label) {
        return LabelResponse.builder()
                .id(label.getId())
                .issueId(label.getIssueId())
                .name(label.getName())
                .createdBy(label.getCreatedBy())
                .createdAt(label.getCreatedAt())
                .build();
    }
}